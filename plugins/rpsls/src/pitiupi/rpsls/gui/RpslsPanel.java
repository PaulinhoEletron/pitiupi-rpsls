package pitiupi.rpsls.gui;

import pitiupi.rpsls.game.GameListener;
import pitiupi.rpsls.game.GameSession.State;
import pitiupi.rpsls.game.Move;
import pitiupi.rpsls.game.Outcome;
import pitiupi.rpsls.game.RoundResult;
import pitiupi.rpsls.game.Rules;
import pitiupi.rpsls.game.Score;
import pitiupi.rpsls.game.Timeouts;
import pitiupi.rpsls.gui.Widgets.CountdownView;
import pitiupi.rpsls.gui.Widgets.MoveBadge;
import pitiupi.rpsls.gui.Widgets.MoveButton;
import pitiupi.rpsls.gui.Widgets.Pentagon;
import pitiupi.rpsls.gui.Widgets.PillButton;
import pitiupi.rpsls.gui.Widgets.TimerBar;
import pitiupi.rpsls.net.Match;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static pitiupi.rpsls.gui.Widgets.label;

/**
 * The plugin's tab inside Pitiupi's main window (registered with
 * {@code bindShellWorkspaceContent}). Fixed header (game name, score, rules)
 * and a {@link CardLayout} with the screens:
 * <pre>
 *   LOBBY   - how to challenge someone, challenge by IP, this machine's IP
 *   INVITE  - "X te desafiou! Bora?" (X challenged you)   [Bora!] [Agora não]
 *   WAIT    - waiting message                             [Cancelar]
 *   PLAY    - 3‑2‑1‑Vai! countdown, time bar and the 5 moves
 *   RESULT  - both moves side by side, the rule, the winner, [Yep] [Nope]
 *   END     - why the match ended                         [Voltar]
 * </pre>
 * One match is shown at a time ({@link #attach}). Notifications from a match
 * that is no longer attached are ignored. All methods run on the Swing thread
 * ({@link Match} guarantees that for game notifications). On-screen text is
 * in Portuguese, like Pitiupi itself.
 */
public class RpslsPanel extends JPanel {

    private static final String LOBBY = "lobby", INVITE = "invite", WAIT = "wait",
            PLAY = "play", RESULT = "result", END = "end";

    private final BiConsumer<String, String> challengeByIp;   // (ip, label)
    private final Supplier<String> localIp;

    private Match match;          // match currently on screen (null = lobby)
    private String myName = "";
    private String peerName = "";

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);

    // header
    private final JLabel scoreLabel = label(" ", Font.BOLD, 15, Theme.TEXT);
    private final JLabel roundLabel = label(" ", Font.PLAIN, 12, Theme.MUTED);

    // lobby
    private final JTextField ipField = new JTextField(14);
    private final JLabel myIpLabel = label(" ", Font.PLAIN, 13, Theme.MUTED);
    private final JLabel lobbyStatus = label(" ", Font.PLAIN, 14, Theme.MUTED);
    private PillButton ipChallenge;

    // invitation / waiting / end
    private final JLabel inviteTitle = label(" ", Font.BOLD, 24, Theme.TEXT);
    private final JLabel waitText = label(" ", Font.PLAIN, 17, Theme.TEXT);
    private final JLabel endText = label(" ", Font.PLAIN, 16, Theme.TEXT);
    private PillButton bora, nao;

    // game
    private final CountdownView countdown = new CountdownView();
    private final TimerBar timerBar = new TimerBar();
    private final JLabel playStatus = label(" ", Font.PLAIN, 15, Theme.MUTED);
    private final MoveButton[] moveButtons = new MoveButton[Move.values().length];
    private Timer countdownTimer;

    // result
    private final MoveBadge myBadge = new MoveBadge();
    private final MoveBadge peerBadge = new MoveBadge();
    private final JLabel resultRule = label(" ", Font.BOLD, 20, Theme.TEXT);
    private final JLabel resultWinner = label(" ", Font.BOLD, 30, Theme.TEAL);
    private final JLabel rematchStatus = label(" ", Font.PLAIN, 14, Theme.MUTED);
    private final PillButton yep = new PillButton("Yep", "rematch-yes", true);
    private final PillButton nope = new PillButton("Nope", "rematch-no", false);

    /**
     * @param challengeByIp called when the player challenges someone by typing an IP
     * @param localIp       this machine's best IPv4, shown in the lobby
     */
    public RpslsPanel(BiConsumer<String, String> challengeByIp, Supplier<String> localIp) {
        super(new BorderLayout());
        this.challengeByIp = challengeByIp;
        this.localIp = localIp;
        setBackground(Theme.BG);

        body.setOpaque(false);
        body.add(buildLobby(), LOBBY);
        body.add(buildInvite(), INVITE);
        body.add(buildWait(), WAIT);
        body.add(buildPlay(), PLAY);
        body.add(buildResult(), RESULT);
        body.add(buildEnd(), END);

        add(buildHeader(), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        showLobby();
    }

    // ================================================================== match binding

    /** Puts {@code match} on screen. Returns the listener that the match must notify. */
    public GameListener attach(Match match, String myName, String peerLabel) {
        stopTimers();
        this.match = match;
        this.myName = myName;
        this.peerName = peerLabel;
        updateHeader(null, 0);
        return new Bound(match);
    }

    /** True while a match (not yet finished) is on screen. */
    public boolean hasLiveMatch() {
        return match != null && match.session().getState() != State.FINISHED;
    }

    /** Lobby status line ("Conectando...", connection errors, ...). */
    public void showLobbyStatus(String text, boolean error) {
        lobbyStatus.setForeground(error ? Theme.LOSE : Theme.MUTED);
        lobbyStatus.setText(html(esc(text).replace("\n", "<br>")));
        ipChallenge.setEnabled(!text.startsWith("Conectando"));
    }

    public void showLobby() {
        stopTimers();
        match = null;
        myIpLabel.setText("Seu IP: " + localIp.get());
        scoreLabel.setText("Pronto para jogar");
        roundLabel.setText(" ");
        lobbyStatus.setText(" ");
        ipChallenge.setEnabled(true);
        cards.show(body, LOBBY);
    }

    /** Forwards notifications only while its match is the one on screen. */
    private final class Bound implements GameListener {
        private final Match owner;

        Bound(Match owner) { this.owner = owner; }

        private boolean current() { return match == owner; }

        @Override public void onStateChanged(State s)            { if (current()) RpslsPanel.this.onStateChanged(s); }
        @Override public void onInvited(String peer)             { if (current()) RpslsPanel.this.onInvited(peer); }
        @Override public void onRoundStarted(int round)          { if (current()) RpslsPanel.this.onRoundStarted(round); }
        @Override public void onAutoChosen(Move move)            { if (current()) RpslsPanel.this.onAutoChosen(move); }
        @Override public void onPeerCommitted()                  { if (current()) RpslsPanel.this.onPeerCommitted(); }
        @Override public void onRoundResult(RoundResult r)       { if (current()) RpslsPanel.this.onRoundResult(r); }
        @Override public void onPeerRematch(boolean yes)         { if (current()) RpslsPanel.this.onPeerRematch(yes); }
        @Override public void onFinished(String reason, boolean byPeer) { if (current()) RpslsPanel.this.onFinished(reason); }
    }

    // ================================================================== screens

    private JComponent buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(Theme.BG_2);
        h.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel brand = new JLabel("PEDRA · PAPEL · TESOURA · LAGARTO · SPOCK");
        brand.setFont(Theme.font(Font.BOLD, 11));
        brand.setForeground(Theme.TEAL);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(scoreLabel);
        center.add(roundLabel);

        JButton rules = new JButton("Regras");
        rules.setName("rules");
        rules.setFont(Theme.font(Font.BOLD, 12));
        rules.setForeground(Theme.MUTED);
        rules.setContentAreaFilled(false);
        rules.setBorderPainted(false);
        rules.setFocusPainted(false);
        rules.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rules.addActionListener(e -> showRules());

        h.add(brand, BorderLayout.WEST);
        h.add(center, BorderLayout.CENTER);
        h.add(rules, BorderLayout.EAST);
        return h;
    }

    private JPanel buildLobby() {
        ipField.setName("ip-field");
        ipField.setFont(Theme.font(Font.PLAIN, 15));
        ipField.setHorizontalAlignment(SwingConstants.CENTER);
        ipField.setMaximumSize(new Dimension(200, 34));
        ipField.setToolTipText("IP da outra pessoa, por exemplo 192.168.0.23");

        ipChallenge = new PillButton("Desafiar", "ip-challenge", true);
        ipChallenge.addActionListener(e -> {
            String ip = ipField.getText().trim();
            if (ip.isEmpty()) {
                showLobbyStatus("Digite o IP da outra pessoa.", true);
                return;
            }
            challengeByIp.accept(ip, ip);
        });
        ipField.addActionListener(e -> ipChallenge.doClick());

        JPanel ipRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        ipRow.setOpaque(false);
        ipRow.add(ipField);
        ipRow.add(ipChallenge);
        ipRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        return column(new Pentagon(),
                label("Pedra, Papel, Tesoura, Lagarto, Spock", Font.BOLD, 22, Theme.TEXT),
                label(html("Para desafiar alguém, clique no nome da pessoa na lista de usuários<br>"
                        + "e escolha <b>Desafiar para Pedra, Papel, Tesoura, Lagarto, Spock</b>."),
                        Font.PLAIN, 14, Theme.MUTED),
                gap(4),
                label("Ou desafie pelo IP:", Font.BOLD, 14, Theme.TEXT),
                ipRow, myIpLabel, lobbyStatus);
    }

    private JPanel buildInvite() {
        bora = new PillButton("Bora!", "invite-accept", true);
        nao = new PillButton("Agora não", "invite-decline", false);
        bora.addActionListener(e -> { setEnabledAll(false, bora, nao); act(s -> s.accept()); });
        nao.addActionListener(e -> { setEnabledAll(false, bora, nao); act(s -> s.decline("agora não")); });
        return column(new Pentagon(), inviteTitle,
                label("E aí, bora jogar Pedra, Papel, Tesoura, Lagarto, Spock?", Font.PLAIN, 16, Theme.MUTED),
                gap(6), row(bora, nao));
    }

    private JPanel buildWait() {
        PillButton cancel = new PillButton("Cancelar", "cancel", false);
        cancel.addActionListener(e -> act(s -> s.quit("desistiu")));
        return column(new Pentagon(), waitText, gap(6), row(cancel));
    }

    private JPanel buildPlay() {
        JPanel buttons = new JPanel(new GridLayout(1, 5, 6, 0));
        buttons.setOpaque(false);
        for (Move m : Move.values()) {
            MoveButton b = new MoveButton(m);
            b.addActionListener(e -> choose(m));
            moveButtons[m.ordinal()] = b;
            buttons.add(b);
        }
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 60, 0, 60));
        bar.add(timerBar);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        bar.setPreferredSize(new Dimension(520, 10));
        return column(countdown, bar, gap(10), buttons, gap(6), playStatus);
    }

    private JPanel buildResult() {
        JPanel vs = new JPanel(new GridBagLayout());
        vs.setOpaque(false);
        JLabel x = label("VS", Font.BOLD, 22, Theme.MUTED);
        x.setBorder(BorderFactory.createEmptyBorder(0, 18, 50, 18));
        vs.add(myBadge);
        vs.add(x);
        vs.add(peerBadge);

        yep.addActionListener(e -> {
            setEnabledAll(false, yep, nope);
            rematchStatus.setText("Aguardando " + peerName + "...");
            act(s -> s.rematch(true));
        });
        nope.addActionListener(e -> { setEnabledAll(false, yep, nope); act(s -> s.rematch(false)); });

        return column(vs, resultRule, resultWinner, gap(4),
                label("Jogar de novo?", Font.BOLD, 15, Theme.TEXT), row(yep, nope), rematchStatus);
    }

    private JPanel buildEnd() {
        PillButton back = new PillButton("Voltar", "close", true);
        back.addActionListener(e -> showLobby());
        return column(label("Fim de jogo", Font.BOLD, 28, Theme.TEXT), endText, gap(8), row(back));
    }

    // ================================================================== game notifications

    private void onStateChanged(State state) {
        switch (state) {
            case INVITING -> showWait("Desafio enviado. Aguardando <b>" + esc(peerName) + "</b> aceitar...");
            case WAITING_COMMIT -> playStatus.setText(html("Aguardando <b>" + esc(peerName) + "</b> escolher..."));
            default -> { }
        }
    }

    private void onInvited(String peer) {
        peerName = peer;
        inviteTitle.setText(html("<b>" + esc(peer) + "</b> te desafiou!"));
        updateHeader(null, 0);
        setEnabledAll(true, bora, nao);
        cards.show(body, INVITE);
    }

    private void onRoundStarted(int round) {
        String peer = match.session().getPeerName();
        if (peer != null) peerName = peer;
        updateHeader(match.session().getScore(), round);

        for (MoveButton b : moveButtons) {
            b.setChosen(false);
            b.setDimmed(false);
            b.setEnabled(false);
        }
        timerBar.stop();
        playStatus.setForeground(Theme.MUTED);
        playStatus.setText("Prepare-se...");
        cards.show(body, PLAY);

        // 3, 2, 1, Vai!  (buttons are enabled on "Vai!")
        String[] steps = {"3", "2", "1", "Vai!"};
        int[] i = {0};
        countdown.show(steps[0]);
        if (countdownTimer != null) countdownTimer.stop();
        countdownTimer = new Timer((int) (Timeouts.COUNTDOWN_MS / 3), e -> {
            i[0]++;
            countdown.show(steps[i[0]]);
            if (i[0] == steps.length - 1) {
                ((Timer) e.getSource()).stop();
                for (MoveButton b : moveButtons) b.setEnabled(true);
                timerBar.start(Timeouts.CHOICE_MS);
                playStatus.setForeground(Theme.TEXT);
                playStatus.setText("Escolha a sua jogada!");
            }
        });
        countdownTimer.start();
    }

    private void choose(Move m) {
        if (!moveButtons[m.ordinal()].isEnabled()) return;
        markChosen(m);
        playStatus.setForeground(Theme.MUTED);
        playStatus.setText(html("Você escolheu <b>" + m.label() + "</b>. Aguardando " + esc(peerName) + "..."));
        act(s -> s.choose(m));
    }

    private void markChosen(Move m) {
        timerBar.stop();
        for (MoveButton b : moveButtons) {
            b.setEnabled(false);
            b.setChosen(b.move == m);
            b.setDimmed(b.move != m);
        }
    }

    private void onAutoChosen(Move move) {
        if (countdownTimer != null) countdownTimer.stop();
        markChosen(move);
        playStatus.setForeground(Theme.DRAW);
        playStatus.setText(html("Tempo esgotado! Jogada aleatória: <b>" + move.label() + "</b>"));
    }

    private void onPeerCommitted() {
        if (moveButtons[0].isEnabled() || "3 2 1".contains(countdown.text())) {
            playStatus.setForeground(Theme.TEAL);
            playStatus.setText(html("<b>" + esc(peerName) + "</b> já escolheu!"));
        }
    }

    private void onRoundResult(RoundResult r) {
        timerBar.stop();
        Color mine, theirs;
        switch (r.outcome()) {
            case WIN -> {
                mine = Theme.WIN; theirs = Theme.LOSE;
                resultWinner.setText(myName + " venceu!");
                resultWinner.setForeground(Theme.WIN);
            }
            case LOSE -> {
                mine = Theme.LOSE; theirs = Theme.WIN;
                resultWinner.setText(peerName + " venceu!");
                resultWinner.setForeground(Theme.LOSE);
            }
            default -> {
                mine = theirs = Theme.DRAW;
                resultWinner.setText("Empate!");
                resultWinner.setForeground(Theme.DRAW);
            }
        }
        myBadge.set(r.myMove(), myName + " (você)", mine, r.outcome() == Outcome.LOSE);
        peerBadge.set(r.peerMove(), peerName, theirs, r.outcome() == Outcome.WIN);
        resultRule.setText(r.explanation());
        updateHeader(r.score(), r.round());
        rematchStatus.setText(" ");
        setEnabledAll(true, yep, nope);
        cards.show(body, RESULT);
    }

    private void onPeerRematch(boolean yes) {
        if (yes && yep.isEnabled()) {
            rematchStatus.setForeground(Theme.TEAL);
            rematchStatus.setText(html("<b>" + esc(peerName) + "</b> quer jogar de novo!"));
        }
    }

    private void onFinished(String reason) {
        stopTimers();
        endText.setText(html(esc(reason).replace("\n", "<br>")));
        cards.show(body, END);
    }

    // ================================================================== helpers

    private void act(java.util.function.Consumer<pitiupi.rpsls.game.GameSession> action) {
        if (match != null) match.act(action);
    }

    private void showWait(String htmlText) {
        waitText.setText(html(htmlText));
        cards.show(body, WAIT);
    }

    private void stopTimers() {
        if (countdownTimer != null) countdownTimer.stop();
        countdown.stop();
        timerBar.stop();
    }

    private void updateHeader(Score s, int round) {
        if (s == null) {
            scoreLabel.setText(html(esc(myName) + " &nbsp;×&nbsp; " + esc(peerName)));
            roundLabel.setText(" ");
        } else {
            scoreLabel.setText(html(esc(myName) + " &nbsp;<b>" + s.wins() + " × " + s.losses() + "</b>&nbsp; " + esc(peerName)));
            roundLabel.setText("Rodada " + round + (s.draws() > 0 ? "  ·  empates: " + s.draws() : ""));
        }
    }

    private void showRules() {
        JPanel p = new JPanel(new GridLayout(0, 2, 18, 6));
        p.setBackground(Theme.BG);
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        for (String rule : Rules.all()) p.add(label(rule, Font.PLAIN, 14, Theme.TEXT));
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "Regras", Dialog.ModalityType.MODELESS);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Theme.BG);
        wrap.add(new Pentagon(), BorderLayout.NORTH);
        wrap.add(p, BorderLayout.CENTER);
        JLabel hint = label("Cada jogada vence 2 e perde para 2.", Font.PLAIN, 12, Theme.MUTED);
        hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        wrap.add(hint, BorderLayout.SOUTH);
        d.setContentPane(wrap);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private static void setEnabledAll(boolean on, JComponent... cs) {
        for (JComponent c : cs) c.setEnabled(on);
    }

    private static JComponent gap(int h) {
        return (JComponent) Box.createVerticalStrut(h);
    }

    private static JPanel row(JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        p.setOpaque(false);
        for (JComponent c : items) p.add(c);
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        return p;
    }

    private static JPanel column(JComponent... items) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
        p.add(Box.createVerticalGlue());
        for (JComponent c : items) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(c);
            p.add(Box.createVerticalStrut(8));
        }
        p.add(Box.createVerticalGlue());
        return p;
    }

    private static String html(String s) {
        return "<html><div style='text-align:center'>" + s + "</div></html>";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
