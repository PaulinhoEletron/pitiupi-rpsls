package pitiupi.rpsls.game;

/**
 * Result of a round, from the local player's point of view.
 *
 * @param explanation the rule that applied, e.g. "Spock vaporiza Pedra" (shown in Portuguese in the UI)
 * @param score       accumulated score after this round
 */
public record RoundResult(int round, Move myMove, Move peerMove,
                          Outcome outcome, String explanation, Score score) {
}
