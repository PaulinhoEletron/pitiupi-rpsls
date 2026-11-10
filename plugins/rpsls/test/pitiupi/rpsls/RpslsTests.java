package pitiupi.rpsls;

import pitiupi.rpsls.game.*;
import pitiupi.rpsls.game.GameSession.Role;
import pitiupi.rpsls.game.GameSession.State;
import pitiupi.rpsls.net.RpslsMessage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Tests for the rules, commit‑reveal, messages and state machine, without
 * network and without GUI. Run with {@code make test}.
 *
 * (Simple runner, no JUnit, to avoid external dependencies. Expected UI
 * strings such as "Spock vaporiza Pedra" are in Portuguese on purpose.)
 */
public class RpslsTests {

    private static int passed, failed;

    public static void main(String[] args) {
        test("rules: 25 combinations", RpslsTests::allCombinations);
        test("rules: the game's 10 relations", RpslsTests::tenRelations);
        test("rules: sentences", RpslsTests::explanations);
        test("commit-reveal", RpslsTests::commitment);
        test("message: serialize and deserialize", RpslsTests::messageRoundTrip);
        test("full match (Spock x Rock)", RpslsTests::fullGame);
        test("match: invited player chooses first", RpslsTests::inviteeChoosesFirst);
        test("match: 25 combinations over the simulated link", RpslsTests::allCombinationsThroughSessions);
        test("rematch: Yep + Yep -> round 2", RpslsTests::rematchYesYes);
        test("rematch: Yep + Nope -> end", RpslsTests::rematchYesNo);
        test("invitation declined", RpslsTests::declined);
        test("cheating: tampered REVEAL is detected", RpslsTests::cheatDetected);
        test("out-of-order message -> protocol error", RpslsTests::outOfOrder);
        test("BYE in the middle of a round", RpslsTests::byeMidGame);
        test("connection lost", RpslsTests::connectionLost);
        test("deadline: unanswered invitation", RpslsTests::timeoutInvite);
        test("deadline: no choice -> random move", RpslsTests::timeoutChoice);
        test("deadline: opponent vanished", RpslsTests::timeoutPeerSilent);
        test("deadline: no rematch answer -> Nope", RpslsTests::timeoutRematch);
        test("stale deadline is ignored", RpslsTests::staleTimeout);

        System.out.printf("%n%d passed, %d failed%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    // ------------------------------------------------------------------ rules

    static void allCombinations() {
        for (Move a : Move.values()) {
            int wins = 0, losses = 0;
            for (Move b : Move.values()) {
                Outcome ab = Rules.play(a, b), ba = Rules.play(b, a);
                if (a == b) check(ab == Outcome.DRAW, a + " x " + b + " should be a draw");
                else check(ab != ba && ab != Outcome.DRAW && ba != Outcome.DRAW, "asymmetry " + a + " x " + b);
                if (ab == Outcome.WIN) wins++;
                if (ab == Outcome.LOSE) losses++;
            }
            check(wins == 2 && losses == 2, a + " must beat 2 and lose to 2");
        }
    }

    static void tenRelations() {
        // Official list (Sheldon Cooper / Sam Kass): winner → loser
        Move[][] wins = {
            {Move.TESOURA, Move.PAPEL}, {Move.PAPEL, Move.PEDRA}, {Move.PEDRA, Move.LAGARTO},
            {Move.LAGARTO, Move.SPOCK}, {Move.SPOCK, Move.TESOURA}, {Move.TESOURA, Move.LAGARTO},
            {Move.LAGARTO, Move.PAPEL}, {Move.PAPEL, Move.SPOCK}, {Move.SPOCK, Move.PEDRA},
            {Move.PEDRA, Move.TESOURA},
        };
        for (Move[] w : wins) {
            check(Rules.play(w[0], w[1]) == Outcome.WIN, w[0] + " should beat " + w[1]);
            check(Rules.play(w[1], w[0]) == Outcome.LOSE, w[1] + " should lose to " + w[0]);
        }
    }

    static void explanations() {
        eq("Spock vaporiza Pedra", Rules.explain(Move.SPOCK, Move.PEDRA));
        eq("Spock vaporiza Pedra", Rules.explain(Move.PEDRA, Move.SPOCK));
        eq("Lagarto come Papel", Rules.explain(Move.PAPEL, Move.LAGARTO));
        eq("Tesoura decapita Lagarto", Rules.explain(Move.TESOURA, Move.LAGARTO));
        eq("Papel e Papel: empate", Rules.explain(Move.PAPEL, Move.PAPEL));
    }

    // ------------------------------------------------------------------ commit-reveal

    static void commitment() {
        String n = Commitment.newNonce();
        String h = Commitment.hash("s1", 1, Move.SPOCK, n);
        check(h.length() == 64, "SHA-256 in hex has 64 characters");
        check(Commitment.verify(h, "s1", 1, Move.SPOCK, n), "correct hash must verify");
        check(!Commitment.verify(h, "s1", 1, Move.PEDRA, n), "swapped move must not verify");
        check(!Commitment.verify(h, "s1", 1, Move.SPOCK, Commitment.newNonce()), "swapped nonce must not verify");
        check(!Commitment.verify(h, "s1", 2, Move.SPOCK, n), "another round must not verify");
        check(!Commitment.verify(h, "s2", 1, Move.SPOCK, n), "another session must not verify");
        check(!n.equals(Commitment.newNonce()), "nonces must be random");
    }

    static void messageRoundTrip() {
        RpslsMessage m = RpslsMessage.reveal("abc", 3, Move.LAGARTO, "ff00");
        RpslsMessage r = wire(m);
        eq(RpslsMessage.Type.REVEAL, r.getType());
        eq("abc", r.getSessionId());
        eq(3, r.getRound());
        eq(Move.LAGARTO, r.getMove());
        eq("ff00", r.getNonce());
        eq(RpslsMessage.PROTOCOL, r.getProtocol());
        check(RpslsMessage.fromBytes(new byte[]{1, 2, 3}) == null, "garbage must become null");
    }

    // ------------------------------------------------------------------ sessions

    static void fullGame() {
        Pair p = new Pair();
        p.a.invite();
        p.pump();
        eq(State.INVITED, p.b.getState());
        eq("alice", p.lb.invitedBy);
        p.b.accept();
        p.pump();
        eq(State.CHOOSING, p.a.getState());
        eq(State.CHOOSING, p.b.getState());
        eq("bob", p.a.getPeerName());

        p.a.choose(Move.SPOCK);
        p.pump();
        eq(State.WAITING_COMMIT, p.a.getState());
        check(p.lb.peerCommitted, "bob must know that alice has chosen");
        p.b.choose(Move.PEDRA);
        p.pump();

        eq(State.RESULT, p.a.getState());
        eq(State.RESULT, p.b.getState());
        eq(Outcome.WIN, p.la.last.outcome());
        eq(Outcome.LOSE, p.lb.last.outcome());
        eq("Spock vaporiza Pedra", p.la.last.explanation());
        eq(Move.PEDRA, p.la.last.peerMove());
        eq(1, p.la.last.score().wins());
        eq(1, p.lb.last.score().losses());
        // message order on the wire
        eq(List.of("A:INVITE", "B:ACCEPT", "A:COMMIT", "B:COMMIT", "B:REVEAL", "A:REVEAL"), p.trace);
    }

    static void inviteeChoosesFirst() {
        Pair p = Pair.playing();
        p.b.choose(Move.TESOURA);
        p.pump();
        p.a.choose(Move.PAPEL);
        p.pump();
        eq(Outcome.LOSE, p.la.last.outcome());
        eq("Tesoura corta Papel", p.la.last.explanation());
    }

    static void allCombinationsThroughSessions() {
        for (Move x : Move.values()) for (Move y : Move.values()) {
            Pair p = Pair.playing();
            p.a.choose(x);
            p.b.choose(y);   // both choose "at the same time" (before any delivery)
            p.pump();
            eq(Rules.play(x, y), p.la.last.outcome());
            eq(Rules.play(y, x), p.lb.last.outcome());
        }
    }

    static void rematchYesYes() {
        Pair p = Pair.playing();
        p.a.choose(Move.PAPEL); p.b.choose(Move.PAPEL); p.pump();
        eq(Outcome.DRAW, p.la.last.outcome());
        p.a.rematch(true); p.pump();
        eq(State.REMATCH_WAIT, p.a.getState());
        check(Boolean.TRUE.equals(p.lb.peerRematch), "bob must see alice's Yep");
        p.b.rematch(true); p.pump();
        eq(State.CHOOSING, p.a.getState());
        eq(State.CHOOSING, p.b.getState());
        eq(2, p.a.getRound());
        eq(2, p.b.getRound());
        p.a.choose(Move.LAGARTO); p.b.choose(Move.SPOCK); p.pump();
        eq(Outcome.WIN, p.la.last.outcome());
        eq(1, p.la.last.score().wins());
        eq(1, p.la.last.score().draws());
    }

    static void rematchYesNo() {
        Pair p = Pair.playing();
        p.a.choose(Move.PAPEL); p.b.choose(Move.PEDRA); p.pump();
        p.a.rematch(true);
        p.b.rematch(false);
        p.pump();
        eq(State.FINISHED, p.a.getState());
        eq(State.FINISHED, p.b.getState());
        check(p.la.finishedByPeer, "for alice, bob ended the match");
    }

    static void declined() {
        Pair p = new Pair();
        p.a.invite(); p.pump();
        p.b.decline("agora não"); p.pump();
        eq(State.FINISHED, p.a.getState());
        check(p.la.finishReason.contains("agora não"), "reason must arrive: " + p.la.finishReason);
    }

    static void cheatDetected() {
        Pair p = Pair.playing();
        // bob "cheats": changes the move in REVEAL after seeing the commit
        p.tamperB = m -> m.getType() == RpslsMessage.Type.REVEAL
                ? RpslsMessage.reveal(m.getSessionId(), m.getRound(), Move.SPOCK, m.getNonce()) : m;
        p.a.choose(Move.PEDRA);
        p.b.choose(Move.TESOURA);
        p.pump();
        eq(State.FINISHED, p.a.getState());
        check(p.la.finishReason.contains("REVEAL não confere"), p.la.finishReason);
        check(p.la.last == null, "there must be no result");
    }

    static void outOfOrder() {
        Pair p = Pair.playing();
        p.a.onMessage(RpslsMessage.reveal(p.a.getSessionId(), 1, Move.PEDRA, "00"));
        eq(State.FINISHED, p.a.getState());
        check(p.la.finishReason.startsWith("Erro de protocolo"), p.la.finishReason);
        p.pump(); // the BYE sent by alice ends bob's session too
        eq(State.FINISHED, p.b.getState());
    }

    static void byeMidGame() {
        Pair p = Pair.playing();
        p.a.choose(Move.SPOCK);
        p.b.quit("fechou a aba do jogo");
        p.pump();
        eq(State.FINISHED, p.a.getState());
        check(p.la.finishedByPeer, "ended by the other side");
        check(p.la.finishReason.contains("fechou a aba do jogo"), p.la.finishReason);
    }

    static void connectionLost() {
        Pair p = Pair.playing();
        p.a.onConnectionLost();
        eq(State.FINISHED, p.a.getState());
        check(p.la.finishedByPeer, "a drop counts as an external end");
    }

    static void timeoutInvite() {
        Pair p = new Pair();
        p.a.invite(); p.pump();
        p.a.onTimeout(State.INVITING, 0); p.pump();
        eq(State.FINISHED, p.a.getState());
        eq(State.FINISHED, p.b.getState());   // BYE reached the invitee
        check(p.lb.finishReason.contains("convite expirou"), p.lb.finishReason);
    }

    static void timeoutChoice() {
        Pair p = Pair.playing();
        p.b.choose(Move.PEDRA);
        p.a.onTimeout(State.CHOOSING, 1);     // alice did not click in time
        p.pump();
        check(p.la.autoChosen != null, "alice must be told about the automatic move");
        eq(State.RESULT, p.a.getState());
        eq(State.RESULT, p.b.getState());
        eq(p.la.autoChosen, p.la.last.myMove());
    }

    static void timeoutPeerSilent() {
        Pair p = Pair.playing();
        p.a.choose(Move.SPOCK); p.pump();
        eq(State.WAITING_COMMIT, p.a.getState());
        p.a.onTimeout(State.WAITING_COMMIT, 1);
        eq(State.FINISHED, p.a.getState());
        check(p.la.finishReason.contains("não respondeu"), p.la.finishReason);
    }

    static void timeoutRematch() {
        Pair p = Pair.playing();
        p.a.choose(Move.SPOCK); p.b.choose(Move.SPOCK); p.pump();
        p.b.rematch(true);
        p.a.onTimeout(State.RESULT, 1);       // alice did not answer → Nope
        p.pump();
        eq(State.FINISHED, p.a.getState());
        eq(State.FINISHED, p.b.getState());
    }

    static void staleTimeout() {
        Pair p = Pair.playing();
        p.a.choose(Move.PAPEL); p.b.choose(Move.PEDRA); p.pump();
        p.a.onTimeout(State.CHOOSING, 1);     // the choice deadline arrived late
        eq(State.RESULT, p.a.getState());
        p.a.onTimeout(State.RESULT, 0);       // wrong round
        eq(State.RESULT, p.a.getState());
    }

    // ------------------------------------------------------------------ infrastructure

    /** Two sessions linked by queues (simulates the TCP connection, including serialization). */
    static final class Pair {
        final Deque<Runnable> wire = new ArrayDeque<>();
        final List<String> trace = new ArrayList<>();
        final Recorder la = new Recorder(), lb = new Recorder();
        UnaryOperator<RpslsMessage> tamperB = UnaryOperator.identity();
        final GameSession a, b;

        Pair() {
            GameSession[] s = new GameSession[2];
            a = s[0] = new GameSession(Role.INVITER, "alice", m -> {
                trace.add("A:" + m.getType());
                RpslsMessage w = wire(m);
                wire.add(() -> s[1].onMessage(w));
            }, la);
            b = s[1] = new GameSession(Role.INVITEE, "bob", m -> {
                trace.add("B:" + m.getType());
                RpslsMessage w = wire(tamperB.apply(m));
                wire.add(() -> s[0].onMessage(w));
            }, lb);
        }

        static Pair playing() {
            Pair p = new Pair();
            p.a.invite(); p.pump();
            p.b.accept(); p.pump();
            return p;
        }

        void pump() {
            while (!wire.isEmpty()) wire.poll().run();
        }
    }

    static final class Recorder implements GameListener {
        String invitedBy, finishReason;
        boolean peerCommitted, finishedByPeer;
        Boolean peerRematch;
        RoundResult last;
        Move autoChosen;

        public void onAutoChosen(Move m) { autoChosen = m; }

        public void onInvited(String peer) { invitedBy = peer; }
        public void onPeerCommitted() { peerCommitted = true; }
        public void onRoundResult(RoundResult r) { last = r; }
        public void onPeerRematch(boolean yes) { peerRematch = yes; }
        public void onFinished(String reason, boolean byPeer) { finishReason = reason; finishedByPeer = byPeer; }
    }

    /** Passes the message through the same bytes that would go over TCP. */
    static RpslsMessage wire(RpslsMessage m) {
        try {
            return RpslsMessage.fromBytes(m.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void test(String name, Runnable body) {
        try {
            body.run();
            passed++;
            System.out.println("  ok   " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("  FAILED " + name + " -> " + t);
        }
    }

    static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    static void eq(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual))
            throw new AssertionError("expected <" + expected + "> but got <" + actual + ">");
    }
}
