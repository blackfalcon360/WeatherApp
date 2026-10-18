package com.blackfalcon.weather;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Turns numbers into text using the chosen language and units. */
public final class Fmt {
    public int lang = Lang.EN;
    public boolean imperial = false, h24 = true;

    public String t(String k) { return Lang.t(lang, k); }

    private static boolean na(double v) { return Double.isNaN(v); }

    public String temp(double c) {
        if (na(c)) return "--";
        double v = imperial ? c * 9.0 / 5.0 + 32.0 : c;
        return String.format(Locale.US, "%d\u00B0", Math.round(v));
    }

    public String windNum(double kmh) {
        if (na(kmh)) return "--";
        return String.valueOf(Math.round(imperial ? kmh * 0.621371 : kmh));
    }

    public String windUnit() { return t(imperial ? "mph" : "kmh"); }

    public String wind(double kmh) { return na(kmh) ? "--" : windNum(kmh) + " " + windUnit(); }

    public String precip(double mm) {
        if (na(mm)) return "--";
        return imperial ? String.format(Locale.US, "%.2f %s", mm / 25.4, t("in")) : String.format(Locale.US, "%.1f %s", mm, t("mm"));
    }

    public String press(double hpa) {
        if (na(hpa)) return "--";
        return imperial ? String.format(Locale.US, "%.2f %s", hpa * 0.02953, t("inhg")) : String.format(Locale.US, "%d %s", Math.round(hpa), t("hpa"));
    }

    public String vis(double metres) {
        if (na(metres)) return "--";
        double v = imperial ? metres / 1609.344 : metres / 1000.0;
        String u = t(imperial ? "mi" : "km");
        return v >= 10 ? String.format(Locale.US, "%d %s", Math.round(v), u) : String.format(Locale.US, "%.1f %s", v, u);
    }

    public String pct(double v) { return na(v) ? "--" : Math.round(v) + "%"; }

    private Calendar cal(long ms, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(ms);
        return c;
    }

    /** "14:00" or "2 PM" */
    public String hour(long ms, TimeZone tz) {
        Calendar c = cal(ms, tz);
        int h = c.get(Calendar.HOUR_OF_DAY);
        if (h24) return String.format(Locale.US, "%02d:00", h);
        return (h % 12 == 0 ? 12 : h % 12) + " " + t(h < 12 ? "am" : "pm");
    }

    /** "14:05" or "2:05 PM" */
    public String clock(long ms, TimeZone tz) {
        Calendar c = cal(ms, tz);
        int h = c.get(Calendar.HOUR_OF_DAY), m = c.get(Calendar.MINUTE);
        if (h24) return String.format(Locale.US, "%02d:%02d", h, m);
        return String.format(Locale.US, "%d:%02d %s", h % 12 == 0 ? 12 : h % 12, m, t(h < 12 ? "am" : "pm"));
    }

    public String weekday(long ms, TimeZone tz) {
        return Lang.pick(lang, Lang.WEEK_EN, Lang.WEEK_UR, Lang.WEEK_AR)[cal(ms, tz).get(Calendar.DAY_OF_WEEK) - 1];
    }

    public String dateLine(long ms, TimeZone tz) {
        Calendar c = cal(ms, tz);
        String[] mo = Lang.pick(lang, Lang.MON_EN, Lang.MON_UR, Lang.MON_AR);
        return weekday(ms, tz) + "\u060C " + c.get(Calendar.DAY_OF_MONTH) + " " + mo[c.get(Calendar.MONTH)];
    }

    /** local calendar day number (so two times on the same local day compare equal) */
    public static int dayKey(long ms, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(ms);
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR);
    }

    public String deviceClock(long ms) { return clock(ms, TimeZone.getDefault()); }

    public String dir(double deg) {
        if (na(deg)) return "";
        String[] d = Lang.pick(lang, Lang.DIR_EN, Lang.DIR_UR, Lang.DIR_AR);
        return d[(int) (Math.round(((deg % 360) + 360) % 360 / 45.0) % 8)];
    }

    public static int uvCat(double uv) { return uv < 3 ? 0 : (uv < 6 ? 1 : (uv < 8 ? 2 : (uv < 11 ? 3 : 4))); }
    public static int[] UV_COLOR = {0xFF2ECC71, 0xFFFBC02D, 0xFFFB8C00, 0xFFE53935, 0xFF8E24AA};

    public static int aqiCat(double aqi) { return aqi <= 50 ? 0 : (aqi <= 100 ? 1 : (aqi <= 150 ? 2 : (aqi <= 200 ? 3 : (aqi <= 300 ? 4 : 5)))); }
    public static int[] AQI_COLOR = {0xFF2ECC71, 0xFFFBC02D, 0xFFFB8C00, 0xFFE53935, 0xFF8E24AA, 0xFF880E4F};

    public String duration(long ms) {
        long m = Math.max(0, ms / 60000);
        return (m / 60) + "h " + String.format(Locale.US, "%02d", m % 60) + "m";
    }

    /** Short rain / snow / storm outlook for the next 24 hours. */
    public String outlook(Wx w, int from) {
        for (int i = from; i < Math.min(w.hn, from + 24); i++) {
            double pop = Double.isNaN(w.hPop[i]) ? 0 : w.hPop[i];
            double pr = Double.isNaN(w.hPrec[i]) ? 0 : w.hPrec[i];
            if (pop >= 60 || pr >= 0.5) {
                int c = w.hCode[i];
                boolean snow = c == 71 || c == 73 || c == 75 || c == 77 || c == 85 || c == 86;
                String k = c >= 95 ? "storm_at" : (snow ? "snow_at" : "rain_at");
                return t(k) + " " + hour(w.hTime[i], w.tz);
            }
        }
        return t("no_rain");
    }
}
