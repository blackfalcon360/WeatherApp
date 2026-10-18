package com.blackfalcon.weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** One forecast for one place, always in metric units (converted when shown). */
public final class Wx {
    public static final double NA = Double.NaN;

    public TimeZone tz = TimeZone.getDefault();
    public long updatedMs;

    // now
    public long curTime;
    public double temp = NA, feels = NA, hum = NA, precip = NA, cloud = NA, press = NA, wind = NA, windDir = NA, gust = NA, dew = NA;
    public int code = 0;
    public boolean isDay = true;

    // hourly
    public int hn;
    public long[] hTime;
    public double[] hTemp, hPop, hPrec, hWind, hUv, hVis;
    public int[] hCode;
    public boolean[] hDay;

    // daily
    public int dn;
    public long[] dTime, dRise, dSet;
    public double[] dMax, dMin, dUv, dPrec, dPop, dWind;
    public int[] dCode;

    // air
    public boolean hasAir = false;
    public double aqi = NA, pm25 = NA, pm10 = NA;

    private static double d(JSONArray a, int i) { return a == null ? NA : a.optDouble(i, NA); }
    private static double[] arr(JSONObject o, String k, int n) {
        JSONArray a = o.optJSONArray(k);
        double[] r = new double[n];
        for (int i = 0; i < n; i++) r[i] = d(a, i);
        return r;
    }

    private static long time(SimpleDateFormat f, String s) {
        if (s == null || s.length() == 0) return 0;
        try {
            Date dt = f.parse(s);
            return dt == null ? 0 : dt.getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    public static Wx parse(String forecast, String air) throws JSONException {
        JSONObject r = new JSONObject(forecast);
        Wx w = new Wx();
        String tzn = r.optString("timezone", "");
        if (tzn.length() > 0) w.tz = TimeZone.getTimeZone(tzn);
        SimpleDateFormat f1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
        SimpleDateFormat f2 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f1.setTimeZone(w.tz);
        f2.setTimeZone(w.tz);

        JSONObject c = r.getJSONObject("current");
        w.curTime = time(f1, c.optString("time", ""));
        w.temp = c.optDouble("temperature_2m", NA);
        w.feels = c.optDouble("apparent_temperature", NA);
        w.hum = c.optDouble("relative_humidity_2m", NA);
        w.precip = c.optDouble("precipitation", NA);
        w.cloud = c.optDouble("cloud_cover", NA);
        w.press = c.optDouble("pressure_msl", NA);
        w.wind = c.optDouble("wind_speed_10m", NA);
        w.windDir = c.optDouble("wind_direction_10m", NA);
        w.gust = c.optDouble("wind_gusts_10m", NA);
        w.dew = c.optDouble("dew_point_2m", NA);
        w.code = c.optInt("weather_code", 0);
        w.isDay = c.optInt("is_day", 1) == 1;

        JSONObject h = r.getJSONObject("hourly");
        JSONArray ht = h.getJSONArray("time");
        w.hn = ht.length();
        w.hTime = new long[w.hn];
        w.hCode = new int[w.hn];
        w.hDay = new boolean[w.hn];
        JSONArray hc = h.optJSONArray("weather_code"), hd = h.optJSONArray("is_day");
        for (int i = 0; i < w.hn; i++) {
            w.hTime[i] = time(f1, ht.optString(i, ""));
            w.hCode[i] = hc == null ? 0 : hc.optInt(i, 0);
            w.hDay[i] = hd == null || hd.optInt(i, 1) == 1;
        }
        w.hTemp = arr(h, "temperature_2m", w.hn);
        w.hPop = arr(h, "precipitation_probability", w.hn);
        w.hPrec = arr(h, "precipitation", w.hn);
        w.hWind = arr(h, "wind_speed_10m", w.hn);
        w.hUv = arr(h, "uv_index", w.hn);
        w.hVis = arr(h, "visibility", w.hn);

        JSONObject dd = r.getJSONObject("daily");
        JSONArray dt = dd.getJSONArray("time");
        w.dn = dt.length();
        w.dTime = new long[w.dn];
        w.dRise = new long[w.dn];
        w.dSet = new long[w.dn];
        w.dCode = new int[w.dn];
        JSONArray dc = dd.optJSONArray("weather_code"), dr = dd.optJSONArray("sunrise"), ds = dd.optJSONArray("sunset");
        for (int i = 0; i < w.dn; i++) {
            w.dTime[i] = time(f2, dt.optString(i, ""));
            w.dRise[i] = dr == null ? 0 : time(f1, dr.optString(i, ""));
            w.dSet[i] = ds == null ? 0 : time(f1, ds.optString(i, ""));
            w.dCode[i] = dc == null ? 0 : dc.optInt(i, 0);
        }
        w.dMax = arr(dd, "temperature_2m_max", w.dn);
        w.dMin = arr(dd, "temperature_2m_min", w.dn);
        w.dUv = arr(dd, "uv_index_max", w.dn);
        w.dPrec = arr(dd, "precipitation_sum", w.dn);
        w.dPop = arr(dd, "precipitation_probability_max", w.dn);
        w.dWind = arr(dd, "wind_speed_10m_max", w.dn);

        if (air != null) {
            try {
                JSONObject a = new JSONObject(air).optJSONObject("current");
                if (a != null) {
                    w.aqi = a.optDouble("us_aqi", NA);
                    w.pm25 = a.optDouble("pm2_5", NA);
                    w.pm10 = a.optDouble("pm10", NA);
                    w.hasAir = !Double.isNaN(w.aqi);
                }
            } catch (JSONException e) { /* air quality is optional */ }
        }
        return w;
    }

    /** index of the hour we are in now (first hourly slot not older than 30 minutes) */
    public int nowIndex(long nowMs) {
        for (int i = 0; i < hn; i++) if (hTime[i] >= nowMs - 30 * 60000L) return i;
        return Math.max(0, hn - 1);
    }
}
