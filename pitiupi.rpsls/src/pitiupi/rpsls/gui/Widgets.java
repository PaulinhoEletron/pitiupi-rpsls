package pitiupi.rpsls.gui;

import pitiupi.rpsls.game.Move;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** Hand-painted components so the game looks the same on every system. */
final class Widgets {

    private Widgets() {}

    /** Rounded button: filled (primary action) or outlined (secondary). */
    static final class PillButton extends JButton {
        private final boolean primary;
        private boolean hover;

        PillButton(String text, String name, boolean primary) {
            super(text);
            this.primary = primary;
            setName(name);
            setFont(Theme.font(Font.BOLD, 15));
            setForeground(primary ? Theme.BG : Theme.TEXT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(Math.max(120, getFontMetrics(getFont()).stringWidth(text) + 48), 42));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D r = new RoundRectangle2D.Double(1.5, 1.5, w - 3, h - 3, h - 3, h - 3);
            float a = isEnabled() ? 1f : 0.35f;
            if (primary) {
                g.setColor(Theme.alpha(hover && isEnabled() ? Theme.TEXT : Theme.TEAL, a));
                g.fill(r);
            } else {
                g.setColor(Theme.alpha(hover && isEnabled() ? Theme.BG_3 : Theme.BG_2, a));
                g.fill(r);
                g.setColor(Theme.alpha(Theme.MUTED, a));
                g.setStroke(new BasicStroke(1.6f));
                g.draw(r);
            }
            g.setFont(getFont());
            g.setColor(Theme.alpha(primary ? Theme.BG : Theme.TEXT, a));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Round button for a move: icon + name. */
    static final class MoveButton extends JButton {
        final Move move;
        private boolean hover;
        private boolean chosen;
        private boolean dimmed;

        MoveButton(Move move) {
            this.move = move;
            setName("move-" + move.name());
            setText(move.label());
            setToolTipText(move.label());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(108, 136));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        void setChosen(boolean chosen) { this.chosen = chosen; repaint(); }
        void setDimmed(boolean dimmed) { this.dimmed = dimmed; repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth();
            double d = Math.min(w - 12, 96);
            double x = (w - d) / 2, y = 6;
            boolean active = isEnabled() && hover;
            float a = dimmed ? 0.28f : (isEnabled() || chosen ? 1f : 0.55f);
            double lift = active ? -3 : 0;

            g.setColor(Theme.alpha(chosen ? Theme.TEAL_DARK : Theme.BG_3, a));
            g.fill(new Ellipse2D.Double(x, y + lift, d, d));
            g.setStroke(new BasicStroke(chosen || active ? 5f : 3f));
            g.setColor(Theme.alpha(chosen || active ? Theme.TEAL : Theme.alpha(Theme.TEAL, 0.55f), a));
            g.draw(new Ellipse2D.Double(x + 1.5, y + 1.5 + lift, d - 3, d - 3));
            MoveIcon.paint(g, move, w / 2.0, y + d / 2 + lift, d * 0.62,
                    Theme.alpha(Theme.TEXT, a), chosen ? Theme.TEAL_DARK : Theme.BG_3);

            g.setFont(Theme.font(Font.BOLD, 14));
            g.setColor(Theme.alpha(chosen ? Theme.TEAL : Theme.TEXT, a));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(move.label(), (w - fm.stringWidth(move.label())) / 2, (int) (y + d + 22));
            g.dispose();
        }
    }

    /** Large move icon (result screen), highlighting the winner. */
    static final class MoveBadge extends JComponent {
        private Move move;
        private String caption = "";
        private Color ring = Theme.TEAL;
        private boolean dim;

        MoveBadge() {
            setPreferredSize(new Dimension(170, 200));
        }

        void set(Move move, String caption, Color ring, boolean dim) {
            this.move = move;
            this.caption = caption;
            this.ring = ring;
            this.dim = dim;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            if (move == null) return;
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth();
            double d = Math.min(w - 16, 140);
            double x = (w - d) / 2, y = 6;
            float a = dim ? 0.45f : 1f;
            if (!dim) {   // winner glow
                for (int i = 3; i >= 1; i--) {
                    g.setColor(Theme.alpha(ring, 0.07f * i));
                    double e = 6 * (4 - i);
                    g.fill(new Ellipse2D.Double(x - e, y - e, d + 2 * e, d + 2 * e));
                }
            }
            g.setColor(Theme.alpha(Theme.BG_3, a));
            g.fill(new Ellipse2D.Double(x, y, d, d));
            g.setStroke(new BasicStroke(6f));
            g.setColor(Theme.alpha(ring, a));
            g.draw(new Ellipse2D.Double(x + 3, y + 3, d - 6, d - 6));
            MoveIcon.paint(g, move, w / 2.0, y + d / 2, d * 0.6, Theme.alpha(Theme.TEXT, a), Theme.BG_3);

            g.setFont(Theme.font(Font.BOLD, 16));
            g.setColor(Theme.alpha(Theme.TEXT, a));
            FontMetrics fm = g.getFontMetrics();
            String top = move.label();
            g.drawString(top, (w - fm.stringWidth(top)) / 2, (int) (y + d + 24));
            g.setFont(Theme.font(Font.PLAIN, 13));
            g.setColor(Theme.alpha(Theme.MUTED, a));
            fm = g.getFontMetrics();
            g.drawString(caption, (w - fm.stringWidth(caption)) / 2, (int) (y + d + 44));
            g.dispose();
        }
    }

    /** Large countdown number with a "pop" animation (3, 2, 1, Vai!). */
    static final class CountdownView extends JComponent {
        private String text = "";
        private long shownAt;
        private final Timer anim = new Timer(16, e -> repaint());

        CountdownView() {
            setPreferredSize(new Dimension(300, 96));
        }

        void show(String text) {
            this.text = text;
            this.shownAt = System.currentTimeMillis();
            if (!anim.isRunning()) anim.start();
            repaint();
        }

        String text() { return text; }

        void stop() { anim.stop(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            long t = System.currentTimeMillis() - shownAt;
            double p = Math.min(1, t / 260.0);
            double scale = 1.5 - 0.5 * (1 - Math.pow(1 - p, 3));    // 1.5 -> 1.0 (ease-out)
            if (p >= 1 && anim.isRunning()) anim.stop();
            boolean go = text.endsWith("!");
            g.setFont(Theme.font(Font.BOLD, go ? 62 : 72));
            FontMetrics fm = g.getFontMetrics();
            g.translate(getWidth() / 2.0, getHeight() / 2.0);
            g.scale(scale, scale);
            g.setColor(Theme.alpha(go ? Theme.TEAL : Theme.TEXT, (float) Math.min(1, 0.3 + p)));
            g.drawString(text, -fm.stringWidth(text) / 2f, (fm.getAscent() - fm.getDescent()) / 2f);
            g.dispose();
        }
    }

    /** Bar that empties to show the time left to choose. */
    static final class TimerBar extends JComponent {
        private long start, duration;
        private boolean running;
        private final Timer tick = new Timer(50, e -> repaint());

        TimerBar() {
            setPreferredSize(new Dimension(420, 10));
        }

        void start(long durationMs) {
            this.start = System.currentTimeMillis();
            this.duration = durationMs;
            this.running = true;
            tick.start();
        }

        void stop() {
            running = false;
            tick.stop();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth(), h = getHeight();
            g.setColor(Theme.BG_2);
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h));
            if (running) {
                double left = Math.max(0, 1 - (System.currentTimeMillis() - start) / (double) duration);
                if (left <= 0) tick.stop();
                g.setColor(left < 0.3 ? Theme.LOSE : Theme.TEAL);
                g.fill(new RoundRectangle2D.Double(0, 0, w * left, h, h, h));
            }
            g.dispose();
        }
    }

    /** Centered text (with line breaks) in the game theme. */
    static JLabel label(String text, int style, float size, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(Theme.font(style, size));
        l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    /** Pentagon with the 5 moves (decoration of the invitation screen). */
    static final class Pentagon extends JComponent {
        Pentagon() {
            setPreferredSize(new Dimension(220, 170));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            double cx = getWidth() / 2.0, cy = getHeight() / 2.0 + 6, r = Math.min(getWidth(), getHeight()) * 0.38;
            // cycle order: each one beats the 2 before it
            Move[] order = Move.values();
            double[][] pts = new double[5][2];
            for (int i = 0; i < 5; i++) {
                double ang = Math.toRadians(-90 + 72 * i);
                pts[i][0] = cx + r * Math.cos(ang);
                pts[i][1] = cy + r * Math.sin(ang);
            }
            g.setColor(Theme.alpha(Theme.TEAL, 0.35f));
            g.setStroke(new BasicStroke(2f));
            for (int i = 0; i < 5; i++) {
                for (int k = 1; k <= 2; k++) {
                    int j = (i + k) % 5;
                    g.drawLine((int) pts[i][0], (int) pts[i][1], (int) pts[j][0], (int) pts[j][1]);
                }
            }
            double d = 46;
            for (int i = 0; i < 5; i++) {
                g.setColor(Theme.BG_3);
                g.fill(new Ellipse2D.Double(pts[i][0] - d / 2, pts[i][1] - d / 2, d, d));
                g.setColor(Theme.TEAL);
                g.setStroke(new BasicStroke(2.5f));
                g.draw(new Ellipse2D.Double(pts[i][0] - d / 2, pts[i][1] - d / 2, d, d));
                MoveIcon.paint(g, order[i], pts[i][0], pts[i][1], d * 0.62, Theme.TEXT, Theme.BG_3);
            }
            g.dispose();
        }
    }
}
