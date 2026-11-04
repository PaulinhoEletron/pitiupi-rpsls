package pitiupi.rpsls.gui;

import java.awt.*;

/** Game colors and fonts (dark navy + teal theme). */
final class Theme {

    private Theme() {}

    static final Color BG        = new Color(0x17, 0x14, 0x3D);
    static final Color BG_2      = new Color(0x22, 0x1E, 0x52);
    static final Color BG_3      = new Color(0x2E, 0x29, 0x68);
    static final Color TEAL      = new Color(0x5E, 0xC8, 0xB8);
    static final Color TEAL_DARK = new Color(0x2F, 0x8F, 0x86);
    static final Color BLUE      = new Color(0x2D, 0x9C, 0xDB);
    static final Color TEXT      = new Color(0xF2, 0xF0, 0xFA);
    static final Color MUTED     = new Color(0xA6, 0xA1, 0xC9);
    static final Color WIN       = new Color(0x5E, 0xE0, 0x9A);
    static final Color LOSE      = new Color(0xFF, 0x6B, 0x6B);
    static final Color DRAW      = new Color(0xF2, 0xC1, 0x4E);

    static Font font(int style, float size) {
        return new Font(Font.SANS_SERIF, style, Math.round(size));
    }

    static void hq(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    static Color alpha(Color c, float a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, Math.round(a * 255))));
    }
}
