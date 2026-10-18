package com.blackfalcon.weather;

/** Colour sets for the light and dark themes. */
public final class Pal {
    public int bg, card, text, sub, edge, accent, rain, cloud;
    public boolean dark;

    public static Pal dark() {
        Pal p = new Pal();
        p.dark = true;
        p.bg = 0xFF0B1020; p.card = 0xFF151B30; p.text = 0xFFEAEFFA; p.sub = 0xFF9AA4BF;
        p.edge = 0xFF262E4A; p.accent = 0xFF4FC3F7; p.rain = 0xFF4FC3F7; p.cloud = 0xFFD0DAEA;
        return p;
    }

    public static Pal light() {
        Pal p = new Pal();
        p.dark = false;
        p.bg = 0xFFF2F5FB; p.card = 0xFFFFFFFF; p.text = 0xFF14192B; p.sub = 0xFF5B6580;
        p.edge = 0xFFDDE3F0; p.accent = 0xFF1976D2; p.rain = 0xFF1E88E5; p.cloud = 0xFFB4C3DA;
        return p;
    }
}
