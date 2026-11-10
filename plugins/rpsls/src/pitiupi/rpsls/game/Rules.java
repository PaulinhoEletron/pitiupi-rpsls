package pitiupi.rpsls.game;

import java.util.EnumMap;
import java.util.Map;

/**
 * Rules of Rock, Paper, Scissors, Lizard, Spock.
 *
 * <p>With the moves in the circular order of {@link Move}, the result is a
 * single computation: {@code d = (a - b + 5) % 5}.</p>
 * <ul>
 *   <li>{@code d == 0} → draw</li>
 *   <li>{@code d == 1 or 2} → {@code a} wins</li>
 *   <li>{@code d == 3 or 4} → {@code b} wins</li>
 * </ul>
 *
 * <p>The 10 relations (sentences shown on the result screen, in Portuguese).
 * English: Scissors cuts Paper, Paper covers Rock, Rock crushes Lizard,
 * Lizard poisons Spock, Spock smashes Scissors, Scissors decapitates Lizard,
 * Lizard eats Paper, Paper disproves Spock, Spock vaporizes Rock,
 * Rock crushes Scissors.</p>
 * <pre>
 *   Tesoura corta Papel        Papel cobre Pedra
 *   Pedra esmaga Lagarto       Lagarto envenena Spock
 *   Spock esmaga Tesoura       Tesoura decapita Lagarto
 *   Lagarto come Papel         Papel refuta Spock
 *   Spock vaporiza Pedra       Pedra quebra Tesoura
 * </pre>
 */
public final class Rules {

    private static final Map<Move, Map<Move, String>> VERBS = new EnumMap<>(Move.class);

    static {
        verb(Move.TESOURA, "corta", Move.PAPEL);
        verb(Move.PAPEL, "cobre", Move.PEDRA);
        verb(Move.PEDRA, "esmaga", Move.LAGARTO);
        verb(Move.LAGARTO, "envenena", Move.SPOCK);
        verb(Move.SPOCK, "esmaga", Move.TESOURA);
        verb(Move.TESOURA, "decapita", Move.LAGARTO);
        verb(Move.LAGARTO, "come", Move.PAPEL);
        verb(Move.PAPEL, "refuta", Move.SPOCK);
        verb(Move.SPOCK, "vaporiza", Move.PEDRA);
        verb(Move.PEDRA, "quebra", Move.TESOURA);
    }

    private Rules() {}

    private static void verb(Move winner, String verb, Move loser) {
        VERBS.computeIfAbsent(winner, k -> new EnumMap<>(Move.class)).put(loser, verb);
    }

    /** The 10 relations in the classic order (for the rules screen). */
    public static java.util.List<String> all() {
        java.util.List<String> out = new java.util.ArrayList<>();
        Move[][] order = {
            {Move.TESOURA, Move.PAPEL}, {Move.PAPEL, Move.PEDRA}, {Move.PEDRA, Move.LAGARTO},
            {Move.LAGARTO, Move.SPOCK}, {Move.SPOCK, Move.TESOURA}, {Move.TESOURA, Move.LAGARTO},
            {Move.LAGARTO, Move.PAPEL}, {Move.PAPEL, Move.SPOCK}, {Move.SPOCK, Move.PEDRA},
            {Move.PEDRA, Move.TESOURA},
        };
        for (Move[] w : order) out.add(explain(w[0], w[1]));
        return out;
    }

    /** Result from the point of view of {@code mine}. */
    public static Outcome play(Move mine, Move theirs) {
        int d = Math.floorMod(mine.ordinal() - theirs.ordinal(), Move.values().length);
        if (d == 0) return Outcome.DRAW;
        return (d == 1 || d == 2) ? Outcome.WIN : Outcome.LOSE;
    }

    /**
     * The rule that applied, in Portuguese, e.g. "Spock vaporiza Pedra".
     * On a draw it returns e.g. "Spock e Spock: empate".
     */
    public static String explain(Move a, Move b) {
        return switch (play(a, b)) {
            case DRAW -> a.label() + " e " + b.label() + ": empate";
            case WIN -> a.label() + " " + VERBS.get(a).get(b) + " " + b.label();
            case LOSE -> b.label() + " " + VERBS.get(b).get(a) + " " + a.label();
        };
    }
}
