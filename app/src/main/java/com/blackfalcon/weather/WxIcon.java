package com.blackfalcon.weather;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Weather pictures drawn in code (no image files). */
public final class WxIcon {
    private WxIcon() { }

    public static final int CLEAR = 0, MAINLY = 1, PARTLY = 2, OVERCAST = 3, FOG = 4, DRIZZLE = 5, RAIN = 6, HEAVY = 7, SNOW = 8, THUNDER = 9;

    public static int cat(int code) {
        switch (code) {
            case 0: return CLEAR;
            case 1: return MAINLY;
            case 2: return PARTLY;
            case 3: return OVERCAST;
            case 45: case 48: return FOG;
            case 51: case 53: case 55: case 56: case 57: return DRIZZLE;
            case 61: case 63: case 66: case 67: case 80: case 81: return RAIN;
            case 65: case 82: return HEAVY;
            case 71: case 73: case 75: case 77: case 85: case 86: return SNOW;
            case 95: case 96: case 99: return THUNDER;
            default: return PARTLY;
        }
    }

    /** Language key for the description of a WMO weather code. */
    public static String key(int code) {
        switch (code) {
            case 0: return "c_clear";
            case 1: return "c_mainly";
            case 2: return "c_partly";
            case 3: return "c_overcast";
            case 45: case 48: return "c_fog";
            case 51: case 53: case 55: return "c_drizzle";
            case 56: case 57: return "c_fdrizzle";
            case 61: return "c_rain_l";
            case 63: return "c_rain";
            case 65: return "c_rain_h";
            case 66: case 67: return "c_frain";
            case 71: return "c_snow_l";
            case 73: return "c_snow";
            case 75: return "c_snow_h";
            case 77: return "c_grains";
            case 80: return "c_show_l";
            case 81: return "c_show";
            case 82: return "c_show_h";
            case 85: case 86: return "c_snowshow";
            case 95: return "c_thunder";
            case 96: case 99: return "c_hail";
            default: return "c_partly";
        }
    }

    /** {top, bottom} colours of the big weather card. */
    public static int[] gradient(int code, boolean day) {
        int c = cat(code);
        if (!day) {
            if (c == CLEAR || c == MAINLY) return new int[]{0xFF0F1B33, 0xFF263B5E};
            if (c == THUNDER) return new int[]{0xFF14161C, 0xFF2B2F3A};
            if (c == RAIN || c == HEAVY || c == DRIZZLE) return new int[]{0xFF1A2430, 0xFF33475A};
            return new int[]{0xFF1B2335, 0xFF3A4660};
        }
        switch (c) {
            case CLEAR: case MAINLY: return new int[]{0xFF2F80ED, 0xFF56CCF2};
            case PARTLY: return new int[]{0xFF4B79C1, 0xFF8EB4E8};
            case OVERCAST: return new int[]{0xFF5F6C86, 0xFF8E9BB5};
            case FOG: return new int[]{0xFF7B879E, 0xFFC5CDDA};
            case DRIZZLE: case RAIN: return new int[]{0xFF456176, 0xFF7B97AB};
            case HEAVY: return new int[]{0xFF2F4454, 0xFF5B7387};
            case SNOW: return new int[]{0xFF7E92A6, 0xFFC9D6E3};
            default: return new int[]{0xFF2A2D36, 0xFF50566A};
        }
    }

    private static int darker(int c, float f) {
        return 0xFF000000 | ((int) (((c >> 16) & 255) * f) << 16) | ((int) (((c >> 8) & 255) * f) << 8) | (int) ((c & 255) * f);
    }

    private static void sun(Canvas c, Paint p, float cx, float cy, float r) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFB300);
        c.drawCircle(cx, cy, r, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(r * 0.24f);
        p.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            float sx = (float) Math.sin(a), sy = (float) -Math.cos(a);
            c.drawLine(cx + sx * r * 1.4f, cy + sy * r * 1.4f, cx + sx * r * 1.8f, cy + sy * r * 1.8f, p);
        }
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStyle(Paint.Style.FILL);
    }

    private static void moon(Canvas c, Paint p, float cx, float cy, float r) {
        Path a = new Path();
        a.addCircle(cx, cy, r, Path.Direction.CW);
        Path b = new Path();
        b.addCircle(cx + r * 0.55f, cy - r * 0.3f, r * 0.85f, Path.Direction.CW);
        a.op(b, Path.Op.DIFFERENCE);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFF5F0D6);
        c.drawPath(a, p);
    }

    private static void cloud(Canvas c, Paint p, float cx, float cy, float w, int col) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        c.drawCircle(cx - 0.20f * w, cy + 0.06f * w, 0.17f * w, p);
        c.drawCircle(cx + 0.02f * w, cy - 0.06f * w, 0.24f * w, p);
        c.drawCircle(cx + 0.24f * w, cy + 0.07f * w, 0.16f * w, p);
        c.drawRoundRect(new RectF(cx - 0.37f * w, cy + 0.06f * w, cx + 0.40f * w, cy + 0.23f * w), 0.085f * w, 0.085f * w, p);
    }

    private static void drops(Canvas c, Paint p, float cx, float cy, float s, int n, int col) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(s * 0.06f);
        p.setColor(col);
        for (int i = 0; i < n; i++) {
            float x = cx + (i - (n - 1) / 2f) * s * 0.2f;
            c.drawLine(x + s * 0.03f, cy, x - s * 0.04f, cy + s * 0.16f, p);
        }
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStyle(Paint.Style.FILL);
    }

    private static void flakes(Canvas c, Paint p, float cx, float cy, float s) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFFFFF);
        for (int i = 0; i < 3; i++) c.drawCircle(cx + (i - 1) * s * 0.2f, cy + (i % 2) * s * 0.07f + s * 0.04f, s * 0.04f, p);
    }

    private static void bolt(Canvas c, Paint p, float cx, float cy, float s) {
        Path b = new Path();
        b.moveTo(cx + s * 0.04f, cy - s * 0.02f);
        b.lineTo(cx - s * 0.10f, cy + s * 0.14f);
        b.lineTo(cx - s * 0.01f, cy + s * 0.14f);
        b.lineTo(cx - s * 0.06f, cy + s * 0.30f);
        b.lineTo(cx + s * 0.12f, cy + s * 0.08f);
        b.lineTo(cx + s * 0.02f, cy + s * 0.08f);
        b.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFD54F);
        c.drawPath(b, p);
    }

    private static void fog(Canvas c, Paint p, float cx, float cy, float s, int col) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(s * 0.06f);
        p.setColor(col);
        for (int i = 0; i < 3; i++) c.drawLine(cx - s * 0.30f + i * s * 0.04f, cy + i * s * 0.11f, cx + s * 0.30f - i * s * 0.04f, cy + i * s * 0.11f, p);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStyle(Paint.Style.FILL);
    }

    /** Draws the picture for a weather code, centred on (cx, cy), inside a square of side s. */
    public static void draw(Canvas c, Paint p, float cx, float cy, float s, int code, boolean day, int cloudCol, int rainCol) {
        int k = cat(code);
        int dark = darker(cloudCol, 0.72f);
        switch (k) {
            case CLEAR:
                if (day) sun(c, p, cx, cy, s * 0.26f); else moon(c, p, cx, cy, s * 0.30f);
                break;
            case MAINLY:
                if (day) sun(c, p, cx - s * 0.06f, cy - s * 0.06f, s * 0.22f); else moon(c, p, cx - s * 0.06f, cy - s * 0.06f, s * 0.26f);
                cloud(c, p, cx + s * 0.18f, cy + s * 0.2f, s * 0.46f, cloudCol);
                break;
            case PARTLY:
                if (day) sun(c, p, cx - s * 0.13f, cy - s * 0.13f, s * 0.19f); else moon(c, p, cx - s * 0.13f, cy - s * 0.13f, s * 0.23f);
                cloud(c, p, cx + s * 0.06f, cy + s * 0.12f, s * 0.78f, cloudCol);
                break;
            case OVERCAST:
                cloud(c, p, cx - s * 0.12f, cy - s * 0.12f, s * 0.58f, dark);
                cloud(c, p, cx + s * 0.04f, cy + s * 0.08f, s * 0.8f, cloudCol);
                break;
            case FOG:
                cloud(c, p, cx, cy - s * 0.14f, s * 0.78f, cloudCol);
                fog(c, p, cx, cy + s * 0.16f, s, cloudCol);
                break;
            case DRIZZLE:
                cloud(c, p, cx, cy - s * 0.12f, s * 0.8f, cloudCol);
                drops(c, p, cx, cy + s * 0.15f, s, 3, rainCol);
                break;
            case RAIN:
                cloud(c, p, cx, cy - s * 0.12f, s * 0.8f, cloudCol);
                drops(c, p, cx, cy + s * 0.14f, s, 3, rainCol);
                break;
            case HEAVY:
                cloud(c, p, cx, cy - s * 0.14f, s * 0.8f, dark);
                drops(c, p, cx, cy + s * 0.12f, s, 4, rainCol);
                break;
            case SNOW:
                cloud(c, p, cx, cy - s * 0.12f, s * 0.8f, cloudCol);
                flakes(c, p, cx, cy + s * 0.15f, s);
                break;
            default:
                cloud(c, p, cx, cy - s * 0.14f, s * 0.8f, dark);
                bolt(c, p, cx, cy + s * 0.08f, s);
        }
    }
}
