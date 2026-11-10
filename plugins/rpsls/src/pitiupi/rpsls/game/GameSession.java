package pitiupi.rpsls.game;

import pitiupi.rpsls.net.RpslsMessage;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;

/**
 * State machine of ONE match between two players.
 *
 * <p>Knows nothing about networking or the GUI: it receives events
 * ({@link #invite()}, {@link #choose(Move)}, {@link #onMessage(RpslsMessage)}...)
 * and produces messages through the {@link Outbox} and notifications through
 * the {@link GameListener}. This lets it be tested by wiring two sessions
 * together in memory.</p>
 *
 * <pre>
 *  INVITER: IDLE --invite()/INVITE--> INVITING --ACCEPT--> CHOOSING
 *                                              --DECLINE--> FINISHED
 *  INVITEE: IDLE --INVITE--> INVITED --accept()/ACCEPT--> CHOOSING
 *                                    --decline()/DECLINE--> FINISHED
 *
 *  CHOOSING --choose(m)/COMMIT--> WAITING_COMMIT   (opponent has not chosen yet)
 *  CHOOSING --choose(m)/COMMIT+REVEAL--> WAITING_REVEAL (already had their COMMIT)
 *  WAITING_COMMIT --COMMIT/REVEAL--> WAITING_REVEAL
 *  WAITING_REVEAL --REVEAL (hash ok)--> RESULT
 *  RESULT --rematch(true)/REMATCH Yep--> REMATCH_WAIT  (or CHOOSING if they already said Yep)
 *  REMATCH_WAIT --REMATCH Yep--> CHOOSING (round+1)
 *  any --Nope / BYE / quit() / error / deadline expired--> FINISHED
 * </pre>
 *
 * <p>Methods are {@code synchronized}: network and GUI events arrive on
 * different threads. Listener callbacks run inside the lock — the GUI must
 * hand them over to the Swing thread.</p>
 *
 * <p>The {@code reason} strings passed to {@code onFinished} and sent in
 * DECLINE/BYE are shown to the player, so they are in Portuguese.</p>
 */
public class GameSession {

    public enum Role { INVITER, INVITEE }

    public enum State {
        IDLE, INVITING, INVITED,
        CHOOSING, WAITING_COMMIT, WAITING_REVEAL,
        RESULT, REMATCH_WAIT,
        FINISHED
    }

    /** Where the session sends its messages (in the plugin: the TCP connection). */
    @FunctionalInterface
    public interface Outbox {
        void send(RpslsMessage message);
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Role role;
    private final String myName;
    private final Outbox outbox;
    private final GameListener listener;

    private State state = State.IDLE;
    private String sessionId;
    private String peerName;
    private int round;

    // current round data
    private Move myMove;
    private String myNonce;
    private String peerHash;

    // rematch
    private boolean myRematch;
    private Boolean peerRematch; // null = no answer yet

    private final Score score = new Score();

    public GameSession(Role role, String myName, Outbox outbox, GameListener listener) {
        this.role = Objects.requireNonNull(role);
        this.myName = myName;
        this.outbox = Objects.requireNonNull(outbox);
        this.listener = listener != null ? listener : new GameListener() {};
    }

    // ==================================================================
    // Local events (GUI)
    // ==================================================================

    /** Challenger: sends the invitation. */
    public synchronized void invite() {
        require(role == Role.INVITER && state == State.IDLE, "invite()");
        sessionId = UUID.randomUUID().toString();
        send(RpslsMessage.invite(sessionId, myName));
        setState(State.INVITING);
    }

    /** Challenged player: "Bora!" ("Let's go!"). */
    public synchronized void accept() {
        require(state == State.INVITED, "accept()");
        send(RpslsMessage.accept(sessionId, myName));
        startRound(1);
    }

    /** Challenged player: declines. */
    public synchronized void decline(String reason) {
        require(state == State.INVITED, "decline()");
        send(RpslsMessage.decline(sessionId, reason));
        finish("Você recusou o convite", false);
    }

    /** The player clicked one of the 5 moves. */
    public synchronized void choose(Move move) {
        require(state == State.CHOOSING, "choose()");
        myMove = Objects.requireNonNull(move);
        myNonce = Commitment.newNonce();
        send(RpslsMessage.commit(sessionId, round, Commitment.hash(sessionId, round, myMove, myNonce)));

        if (peerHash != null) {          // they had already chosen
            revealAndWait();
        } else {
            setState(State.WAITING_COMMIT);
        }
    }

    /** Answer to "Jogar de novo?" ("Play again?"): Yep (true) or Nope (false). */
    public synchronized void rematch(boolean yes) {
        require(state == State.RESULT, "rematch()");
        myRematch = yes;
        send(RpslsMessage.rematch(sessionId, round, yes));
        if (!yes) {
            finish("Você encerrou a partida", false);
        } else if (Boolean.TRUE.equals(peerRematch)) {
            startRound(round + 1);
        } else {
            setState(State.REMATCH_WAIT);
        }
    }

    /** Leave at any time (e.g. closing the window). */
    public synchronized void quit(String reason) {
        if (state == State.FINISHED || state == State.IDLE) return;
        send(RpslsMessage.bye(sessionId, reason));
        finish(reason, false);
    }

    /** The TCP connection dropped. */
    public synchronized void onConnectionLost() {
        if (state == State.FINISHED) return;
        finish("Conexão com o adversário perdida", true);
    }

    /**
     * The deadline of state {@code expected} in round {@code expectedRound}
     * expired. If the session has already moved to another state/round, the
     * notice is stale and ignored. Policy per state:
     * <ul>
     *   <li>INVITING: nobody answered → BYE "convite expirou" (invitation expired)</li>
     *   <li>INVITED: I did not answer → DECLINE "tempo esgotado" (time is up)</li>
     *   <li>CHOOSING: I did not choose → random move (the game never hangs)</li>
     *   <li>WAITING_COMMIT / WAITING_REVEAL / REMATCH_WAIT: the opponent vanished → BYE</li>
     *   <li>RESULT: I did not answer "Play again?" → Nope</li>
     * </ul>
     */
    public synchronized void onTimeout(State expected, int expectedRound) {
        if (state != expected || round != expectedRound) return;
        switch (state) {
            case INVITING -> {
                send(RpslsMessage.bye(sessionId, "convite expirou"));
                finish("Convite expirou: " + nameOrPeer() + " não respondeu.\n"
                        + "Ele tem o plugin RPSLS instalado? (sem o plugin, o Pitiupi dele descarta o convite)", false);
            }
            case INVITED -> {
                send(RpslsMessage.decline(sessionId, "tempo esgotado"));
                finish("Convite expirou", false);
            }
            case CHOOSING -> {
                Move move = Move.values()[RANDOM.nextInt(Move.values().length)];
                listener.onAutoChosen(move);
                choose(move);
            }
            case WAITING_COMMIT, WAITING_REVEAL, REMATCH_WAIT -> {
                send(RpslsMessage.bye(sessionId, "tempo esgotado"));
                finish(nameOrPeer() + " não respondeu a tempo", false);
            }
            case RESULT -> rematch(false);
            default -> { }
        }
    }

    // ==================================================================
    // Network events
    // ==================================================================

    public synchronized void onMessage(RpslsMessage msg) {
        if (msg == null || state == State.FINISHED) return;

        switch (msg.getType()) {
            case INVITE -> onInvite(msg);
            case ACCEPT -> onAccept(msg);
            case DECLINE -> onDecline(msg);
            case COMMIT -> onCommit(msg);
            case REVEAL -> onReveal(msg);
            case REMATCH -> onRematch(msg);
            case BYE -> onBye(msg);
        }
    }

    private void onInvite(RpslsMessage msg) {
        if (!expect(role == Role.INVITEE && state == State.IDLE, msg)) return;
        sessionId = msg.getSessionId();
        peerName = msg.getSenderName();
        setState(State.INVITED);
        listener.onInvited(peerName);
    }

    private void onAccept(RpslsMessage msg) {
        if (!expect(state == State.INVITING && sameSession(msg), msg)) return;
        peerName = msg.getSenderName();
        startRound(1);
    }

    private void onDecline(RpslsMessage msg) {
        if (!expect(state == State.INVITING && sameSession(msg), msg)) return;
        finish("Convite recusado" + suffix(msg.getReason()), true);
    }

    private void onCommit(RpslsMessage msg) {
        boolean ok = sameRound(msg) && peerHash == null
                && (state == State.CHOOSING || state == State.WAITING_COMMIT);
        if (!expect(ok, msg)) return;

        peerHash = msg.getHash();
        listener.onPeerCommitted();
        if (state == State.WAITING_COMMIT) revealAndWait();
        // if I am still CHOOSING, I reveal when I choose
    }

    private void onReveal(RpslsMessage msg) {
        if (!expect(state == State.WAITING_REVEAL && sameRound(msg), msg)) return;

        Move peerMove = msg.getMove();
        if (!Commitment.verify(peerHash, sessionId, round, peerMove, msg.getNonce())) {
            protocolError("REVEAL não confere com o COMMIT (jogada alterada?)");
            return;
        }

        Outcome outcome = Rules.play(myMove, peerMove);
        score.add(outcome);
        setState(State.RESULT);
        listener.onRoundResult(new RoundResult(
                round, myMove, peerMove, outcome, Rules.explain(myMove, peerMove), score.copy()));
    }

    private void onRematch(RpslsMessage msg) {
        boolean ok = sameRound(msg) && peerRematch == null
                && (state == State.RESULT || state == State.REMATCH_WAIT);
        if (!expect(ok, msg)) return;

        peerRematch = msg.isAccept();
        listener.onPeerRematch(peerRematch);
        if (!peerRematch) {
            finish(nameOrPeer() + " não quis jogar de novo", true);
        } else if (state == State.REMATCH_WAIT && myRematch) {
            startRound(round + 1);
        }
    }

    private void onBye(RpslsMessage msg) {
        if (!sameSession(msg) && sessionId != null) return;
        finish(nameOrPeer() + " saiu" + suffix(msg.getReason()), true);
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private void startRound(int number) {
        round = number;
        myMove = null;
        myNonce = null;
        peerHash = null;
        myRematch = false;
        peerRematch = null;
        setState(State.CHOOSING);
        listener.onRoundStarted(round);
    }

    private void revealAndWait() {
        send(RpslsMessage.reveal(sessionId, round, myMove, myNonce));
        setState(State.WAITING_REVEAL);
    }

    private boolean sameSession(RpslsMessage msg) {
        return sessionId != null && sessionId.equals(msg.getSessionId());
    }

    private boolean sameRound(RpslsMessage msg) {
        return sameSession(msg) && msg.getRound() == round;
    }

    /** Unexpected message → protocol error (ends the session). */
    private boolean expect(boolean condition, RpslsMessage msg) {
        if (!condition) protocolError(msg.getType() + " inesperado no estado " + state);
        return condition;
    }

    private void protocolError(String detail) {
        send(RpslsMessage.bye(sessionId, "erro de protocolo: " + detail));
        finish("Erro de protocolo: " + detail, false);
    }

    private void finish(String reason, boolean byPeer) {
        setState(State.FINISHED);
        listener.onFinished(reason, byPeer);
    }

    private void send(RpslsMessage msg) {
        outbox.send(msg);
    }

    private void setState(State next) {
        state = next;
        listener.onStateChanged(next);
    }

    private void require(boolean condition, String action) {
        if (!condition) throw new IllegalStateException(action + " não é permitido no estado " + state);
    }

    private String nameOrPeer() {
        return peerName != null ? peerName : "O adversário";
    }

    private static String suffix(String reason) {
        return (reason == null || reason.isBlank()) ? "" : " (" + reason + ")";
    }

    // ==================================================================

    public synchronized State getState()   { return state; }
    public synchronized String getSessionId() { return sessionId; }
    public synchronized String getPeerName() { return peerName; }
    public String getMyName()              { return myName; }
    public synchronized int getRound()     { return round; }
    public synchronized Score getScore()   { return score.copy(); }
    public Role getRole()                  { return role; }
}
