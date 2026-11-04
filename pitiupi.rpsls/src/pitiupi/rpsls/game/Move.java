package pitiupi.rpsls.game;

/**
 * The five moves (names in Portuguese: Pedra = Rock, Papel = Paper,
 * Tesoura = Scissors, Lagarto = Lizard, Spock = Spock).
 *
 * <p>The order of the constants is NOT arbitrary: it is the "cycle" used by
 * {@link Rules}. Each move beats the two moves right before it in the cycle
 * and loses to the two right after it.</p>
 *
 * <pre>
 *   PEDRA(0) → SPOCK(1) → PAPEL(2) → LAGARTO(3) → TESOURA(4) → (wraps around)
 * </pre>
 */
public enum Move {
    PEDRA("Pedra"),
    SPOCK("Spock"),
    PAPEL("Papel"),
    LAGARTO("Lagarto"),
    TESOURA("Tesoura");

    private final String label;

    Move(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
