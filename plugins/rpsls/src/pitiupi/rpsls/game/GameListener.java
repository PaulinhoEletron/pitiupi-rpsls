package pitiupi.rpsls.game;

/**
 * Notifications that a {@link GameSession} sends to its user (the GUI).
 * All methods have empty defaults; override only what you need.
 */
public interface GameListener {

    /** An invitation arrived from {@code peerName}: show "Bora jogar?" ("Let's play?"). */
    default void onInvited(String peerName) {}

    /** A round started: show the 3‑2‑1 countdown and the 5 buttons. */
    default void onRoundStarted(int round) {}

    /** The time to choose ran out and the game picked {@code move} for me. */
    default void onAutoChosen(Move move) {}

    /** The opponent has chosen (without revealing what). */
    default void onPeerCommitted() {}

    /** Both moves were revealed and verified. */
    default void onRoundResult(RoundResult result) {}

    /** The opponent answered "Jogar de novo?" ("Play again?"). */
    default void onPeerRematch(boolean yes) {}

    /** The match is over. {@code byPeer} = the other side ended it. */
    default void onFinished(String reason, boolean byPeer) {}

    /** Every state change (useful for logs and tests). */
    default void onStateChanged(GameSession.State state) {}
}
