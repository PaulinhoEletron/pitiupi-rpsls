package pitiupi.rpsls.game;

/** Match score (wins, losses and draws of the local player). */
public final class Score {

    private int wins;
    private int losses;
    private int draws;

    void add(Outcome outcome) {
        switch (outcome) {
            case WIN -> wins++;
            case LOSE -> losses++;
            case DRAW -> draws++;
        }
    }

    Score copy() {
        Score s = new Score();
        s.wins = wins;
        s.losses = losses;
        s.draws = draws;
        return s;
    }

    public int wins()   { return wins; }
    public int losses() { return losses; }
    public int draws()  { return draws; }

    @Override
    public String toString() {
        return wins + " x " + losses + (draws > 0 ? " (" + draws + " empate" + (draws > 1 ? "s" : "") + ")" : "");
    }
}
