package pitiupi.rpsls.game;

/**
 * Protocol deadlines (in milliseconds). When a deadline expires,
 * {@link GameSession#onTimeout} applies that state's policy.
 */
public final class Timeouts {

    private Timeouts() {}

    /** Duration of the on-screen "3, 2, 1, Vai!" ("Go!") countdown. */
    public static final long COUNTDOWN_MS = 3_000;

    /** Time to choose after "Vai!". After that a random move is played. */
    public static final long CHOICE_MS = 10_000;

    /** Time to answer an invitation (both sides). */
    public static final long INVITE_MS = 30_000;

    /** Time waiting for the opponent's COMMIT (they also have the choice deadline). */
    public static final long PEER_COMMIT_MS = COUNTDOWN_MS + CHOICE_MS + 10_000;

    /** Time waiting for the opponent's REVEAL (automatic, should be immediate). */
    public static final long PEER_REVEAL_MS = 10_000;

    /** Time to answer "Jogar de novo?" ("Play again?"). After that it counts as Nope. */
    public static final long REMATCH_MS = 60_000;

    /** Time waiting for the opponent to answer "Play again?". */
    public static final long PEER_REMATCH_MS = REMATCH_MS + 10_000;

    /** Deadline of each state; 0 = no deadline. */
    public static long forState(GameSession.State state) {
        return switch (state) {
            case INVITING, INVITED -> INVITE_MS;
            case CHOOSING -> COUNTDOWN_MS + CHOICE_MS;
            case WAITING_COMMIT -> PEER_COMMIT_MS;
            case WAITING_REVEAL -> PEER_REVEAL_MS;
            case RESULT -> REMATCH_MS;
            case REMATCH_WAIT -> PEER_REMATCH_MS;
            case IDLE, FINISHED -> 0;
        };
    }
}
