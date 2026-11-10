package pitiupi.rpsls.gui;

import pitiupi.rpsls.game.Move;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Icons of the 5 moves.
 *
 * <p>Uses the 3D images from Fluent Emoji (Microsoft, MIT license) packaged
 * in the jar under {@code pitiupi/rpsls/gui/img/}. If an image cannot be
 * loaded, it draws its own Java2D icon instead (a 100×100 frame scaled to the
 * requested size), so the game is never left without icons.</p>
 */
final class MoveIcon {

    private MoveIcon() {}

    private static final Map<Move, BufferedImage> IMAGES = new EnumMap<>(Move.class);
    private static final Map<String, Image> SCALED = new HashMap<>();

    static {
        for (Move m : Move.values()) {
            String file = "img/" + m.name().toLowerCase() + ".png";
            try (InputStream in = MoveIcon.class.getResourceAsStream(file)) {
                if (in != null) IMAGES.put(m, ImageIO.read(in));
            } catch (Exception ignored) {
                // no image: fall back to the Java2D drawing
            }
        }
    }

    /** High-quality resized image (cached per size). */
    private static Image scaled(Move m, int px) {
        return SCALED.computeIfAbsent(m + "@" + px,
                k -> IMAGES.get(m).getScaledInstance(px, px, Image.SCALE_SMOOTH));
    }

    /** Draws the icon of {@code move} centered at (cx, cy) with side {@code size}. */
    static void paint(Graphics2D g0, Move move, double cx, double cy, double size, Color ink, Color detail) {
        if (IMAGES.containsKey(move)) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int px = (int) Math.round(size * 1.12);   // the emoji has inner padding
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ink.getAlpha() / 255f));
            g.drawImage(scaled(move, px), (int) Math.round(cx - px / 2.0), (int) Math.round(cy - px / 2.0), null);
            g.dispose();
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        Theme.hq(g);
        g.translate(cx - size / 2, cy - size / 2);
        g.scale(size / 100.0, size / 100.0);
        switch (move) {
            case PEDRA -> rock(g, ink, detail);
            case PAPEL -> paper(g, ink, detail);
            case TESOURA -> scissors(g, ink, detail);
            case LAGARTO -> lizard(g, ink);
            case SPOCK -> salute(g, ink, detail);
        }
        g.dispose();
    }

    private static Stroke round(float w) {
        return new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    // Rock: rounded stone with cracks
    private static void rock(Graphics2D g, Color ink, Color detail) {
        Path2D p = new Path2D.Double();
        p.moveTo(16, 66);
        p.curveTo(12, 50, 22, 34, 36, 28);
        p.curveTo(46, 18, 64, 18, 74, 30);
        p.curveTo(88, 38, 92, 56, 86, 68);
        p.curveTo(82, 80, 66, 84, 50, 83);
        p.curveTo(34, 84, 20, 80, 16, 66);
        p.closePath();
        g.setColor(ink);
        g.fill(p);
        g.setColor(detail);
        g.setStroke(round(3.2f));
        Path2D c = new Path2D.Double();
        c.moveTo(58, 21); c.lineTo(54, 36); c.lineTo(62, 46); c.lineTo(58, 58);
        c.moveTo(54, 36); c.lineTo(44, 42);
        g.draw(c);
    }

    // Paper: sheet with a folded corner and lines
    private static void paper(Graphics2D g, Color ink, Color detail) {
        AffineTransform old = g.getTransform();
        g.rotate(Math.toRadians(-8), 50, 50);
        Path2D p = new Path2D.Double();
        p.moveTo(26, 14); p.lineTo(62, 14); p.lineTo(78, 30); p.lineTo(78, 86); p.lineTo(26, 86); p.closePath();
        g.setColor(ink);
        g.fill(p);
        g.setColor(detail);
        Path2D fold = new Path2D.Double();
        fold.moveTo(62, 14); fold.lineTo(62, 30); fold.lineTo(78, 30); fold.closePath();
        g.fill(fold);
        g.setStroke(round(3.5f));
        for (int y = 44; y <= 74; y += 10) {
            g.draw(new Line2D.Double(35, y, y == 74 ? 56 : 69, y));
        }
        g.setTransform(old);
    }

    // Scissors: two crossed blades and two handles
    private static void scissors(Graphics2D g, Color ink, Color detail) {
        g.setColor(ink);
        Path2D b1 = new Path2D.Double();          // blade 1 (up-right)
        b1.moveTo(40, 60); b1.lineTo(84, 14); b1.lineTo(88, 20); b1.lineTo(48, 66); b1.closePath();
        Path2D b2 = new Path2D.Double();          // blade 2 (down-right)
        b2.moveTo(40, 40); b2.lineTo(88, 80); b2.lineTo(84, 86); b2.lineTo(34, 48); b2.closePath();
        g.fill(b1);
        g.fill(b2);
        g.setStroke(round(7f));
        g.draw(new Ellipse2D.Double(10, 52, 26, 26));   // lower handle
        g.draw(new Ellipse2D.Double(10, 22, 26, 26));   // upper handle
        g.draw(new Line2D.Double(32, 70, 44, 58));
        g.draw(new Line2D.Double(32, 30, 40, 40));
        g.setColor(detail);
        g.fill(new Ellipse2D.Double(41, 46, 8, 8));     // pivot
    }

    // Lizard: silhouette seen from above
    private static void lizard(Graphics2D g, Color ink) {
        g.setColor(ink);
        // head
        g.fill(new Ellipse2D.Double(55, 6, 20, 25));
        // body
        g.setStroke(round(17f));
        Path2D body = new Path2D.Double();
        body.moveTo(64, 26); body.curveTo(62, 40, 54, 52, 50, 62);
        g.draw(body);
        // tapering tail
        float[] w = {13f, 9.5f, 6.5f, 4f};
        double[][] seg = {{50, 62, 46, 72}, {46, 72, 38, 80}, {38, 80, 28, 84}, {28, 84, 18, 82}};
        for (int i = 0; i < seg.length; i++) {
            g.setStroke(round(w[i]));
            g.draw(new Line2D.Double(seg[i][0], seg[i][1], seg[i][2], seg[i][3]));
        }
        // (bent) legs
        g.setStroke(round(6.5f));
        Path2D legs = new Path2D.Double();
        legs.moveTo(62, 34); legs.lineTo(76, 32); legs.lineTo(82, 24);   // front right
        legs.moveTo(60, 36); legs.lineTo(46, 30); legs.lineTo(42, 20);   // front left
        legs.moveTo(53, 56); legs.lineTo(66, 62); legs.lineTo(70, 72);   // back right
        legs.moveTo(51, 58); legs.lineTo(38, 54); legs.lineTo(30, 60);   // back left
        g.draw(legs);
    }

    // Spock: open hand with fingers split in a "V" (salute)
    private static void salute(Graphics2D g, Color ink, Color detail) {
        g.setColor(ink);
        g.fill(new RoundRectangle2D.Double(30, 46, 42, 44, 18, 18));     // palm
        g.setStroke(round(10.5f));
        // fingers in pairs (index+middle | ring+little) with the "V" in between
        g.draw(new Line2D.Double(35, 54, 27, 17));   // index
        g.draw(new Line2D.Double(44, 52, 37, 11));   // middle
        g.draw(new Line2D.Double(60, 52, 69, 12));   // ring
        g.draw(new Line2D.Double(68, 56, 79, 21));   // little
        g.setStroke(round(11f));
        g.draw(new Line2D.Double(68, 76, 88, 60));   // thumb
        g.setColor(detail);
        g.setStroke(round(2.5f));
        g.draw(new Line2D.Double(38, 74, 56, 80));   // palm line
    }
}
