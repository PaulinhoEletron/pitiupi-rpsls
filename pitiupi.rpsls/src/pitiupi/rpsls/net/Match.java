package pitiupi.rpsls.net;

import pitiupi.net.tcp.TcpConnection;
import pitiupi.rpsls.game.GameListener;
import pitiupi.rpsls.game.GameSession;
import pitiupi.rpsls.game.GameSession.Role;
import pitiupi.rpsls.game.GameSession.State;
import pitiupi.rpsls.game.Move;
import pitiupi.rpsls.game.RoundResult;
import pitiupi.rpsls.game.Timeouts;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Binds a {@link GameSession} to a Pitiupi {@link TcpConnection}.
 *
 * <ul>
 *   <li>messages the session wants to send → {@code connection.send(bytes)};</li>
 *   <li>bytes received by the plugin → {@code session.onMessage};</li>
 *   <li>on every state change, schedules the new state's deadline
 *       ({@link Timeouts}) and cancels the previous one;</li>
 *   <li>forwards the session's notifications to the GUI, on the Swing thread.</li>
 * </ul>
 */
public final class Match implements GameListener {

    private static final Logger LOG = Logger.getLogger(Match.class.getName());

    private final TcpConnection connection;
    private final ScheduledExecutorService timer;
    private final Consumer<Match> onEnded;
    private final GameSession session;
    private volatile GameListener view = new GameListener() {};
    private ScheduledFuture<?> deadline;

    public Match(TcpConnection connection, Role role, String myName,
                 ScheduledExecutorService timer, Consumer<Match> onEnded) {
        this.connection = connection;
        this.timer = timer;
        this.onEnded = onEnded;
        this.session = new GameSession(role, myName, this::send, this);
    }

    /** The GUI that receives the notifications (on the Swing thread). */
    public void setView(GameListener view) {
        this.view = view;
    }

    public GameSession session()        { return session; }
    public TcpConnection connection()   { return connection; }
    public String peerAddress()         { return connection.getAddress().getHostAddress(); }

    // ------------------------------------------------------------------ network

    public void onBytes(byte[] bytes) {
        RpslsMessage msg = RpslsMessage.fromBytes(bytes);
        if (msg == null) {
            LOG.warning("[RPSLS] ignored bytes (not RPSLS/1.0) from " + peerAddress());
            return;
        }
        LOG.info("[RPSLS] <- " + peerAddress() + "  " + msg);
        session.onMessage(msg);
    }

    public void onSocketClosed() {
        session.onConnectionLost();
    }

    private void send(RpslsMessage msg) {
        LOG.info("[RPSLS] -> " + peerAddress() + "  " + msg);
        try {
            connection.send(msg.toByteArray());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "[RPSLS] failed to send " + msg.getType(), e);
            // outside the session lock, so the transition in progress is not interrupted
            timer.execute(session::onConnectionLost);
        }
    }

    // ------------------------------------------------------------------ GUI actions

    /** Runs a player action, ignoring clicks that arrived too late. */
    public void act(Consumer<GameSession> action) {
        timer.execute(() -> {
            try {
                action.accept(session);
            } catch (IllegalStateException late) {
                LOG.fine("[RPSLS] action ignored: " + late.getMessage());
            }
        });
    }

    // ------------------------------------------------------------------ session notifications

    @Override
    public void onStateChanged(State state) {
        LOG.info("[RPSLS] state: " + state);
        if (deadline != null) deadline.cancel(false);
        long ms = Timeouts.forState(state);
        if (ms > 0) {
            int round = session.getRound();
            deadline = timer.schedule(() -> session.onTimeout(state, round), ms, TimeUnit.MILLISECONDS);
        }
        GameListener v = view;
        ui(() -> v.onStateChanged(state));
    }

    @Override public void onInvited(String peer)          { GameListener v = view; ui(() -> v.onInvited(peer)); }
    @Override public void onRoundStarted(int round)       { GameListener v = view; ui(() -> v.onRoundStarted(round)); }
    @Override public void onAutoChosen(Move move)         { GameListener v = view; ui(() -> v.onAutoChosen(move)); }
    @Override public void onPeerCommitted()               { GameListener v = view; ui(v::onPeerCommitted); }
    @Override public void onRoundResult(RoundResult r)    { GameListener v = view; ui(() -> v.onRoundResult(r)); }
    @Override public void onPeerRematch(boolean yes)      { GameListener v = view; ui(() -> v.onPeerRematch(yes)); }

    @Override
    public void onFinished(String reason, boolean byPeer) {
        LOG.info("[RPSLS] match over: " + reason);
        if (deadline != null) deadline.cancel(false);
        onEnded.accept(this);
        GameListener v = view;
        ui(() -> v.onFinished(reason, byPeer));
    }

    private static void ui(Runnable r) {
        SwingUtilities.invokeLater(r);
    }
}
