package com.blackfalcon.weather;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {

    private static final long FRESH_MS = 10 * 60 * 1000L, AUTO_MS = 20 * 60 * 1000L;

    static final class Place {
        String name;
        double lat, lon;
        Place(String n, double la, double lo) { name = n; lat = la; lon = lo; }
        String key() { return String.format(Locale.US, "%.2f,%.2f", lat, lon); }
    }

    private SharedPreferences prefs;
    private LocationManager lm;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newCachedThreadPool();

    private Pal P;
    private final Fmt fmt = new Fmt();
    private int themeMode, tab;

    // places
    private final List<Place> saved = new ArrayList<>();
    private List<Place> results = new ArrayList<>();
    private int sel = -1;                       // -1 = GPS
    private Place gps;
    private boolean geoBusy = false, running = false, searching = false;
    private String searchMsg = "";

    // weather
    private Wx wx;
    private String wxKey = "";
    private boolean fetching = false, offline = false;

    // views
    private final TextView[] tabs = new TextView[3];
    private final View[] pages = new View[3];
    private ScrollView wxScroll, placesScroll;
    private LinearLayout wxBox, placesBox;
    private EditText searchEt;

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private String t(String k) { return fmt.t(k); }

    // ------------------------------------------------------------ callbacks
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (wx == null || System.currentTimeMillis() - wx.updatedMs > AUTO_MS) loadWeather(false);
            handler.postDelayed(this, 60000);
        }
    };

    private final LocationListener locListener = new LocationListener() {
        @Override public void onLocationChanged(Location l) { onGps(l.getLatitude(), l.getLongitude()); }
        @Override public void onProviderEnabled(String p) { }
        @Override public void onProviderDisabled(String p) { }
        @Override public void onStatusChanged(String p, int s, Bundle e) { }
    };

    // ------------------------------------------------------------- lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("weather", MODE_PRIVATE);
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        String country = Locale.getDefault().getCountry();
        boolean us = country.equals("US") || country.equals("LR") || country.equals("MM");
        fmt.lang = prefs.getInt("lang", Lang.UR);
        fmt.imperial = prefs.getBoolean("imperial", us);
        fmt.h24 = prefs.getBoolean("h24", false);
        themeMode = prefs.getInt("theme", 0);
        tab = prefs.getInt("tab", 0);
        P = resolvePal();

        loadPlaces();
        sel = prefs.getInt("sel", -1);
        if (sel >= saved.size()) sel = -1;
        if (prefs.contains("glat")) {
            gps = new Place(prefs.getString("gname", ""), Double.longBitsToDouble(prefs.getLong("glat", 0)), Double.longBitsToDouble(prefs.getLong("glon", 0)));
        }
        buildUi();
        Place pl = currentPlace();
        if (pl != null) {
            wxKey = pl.key();
            wx = readCache(wxKey);
        }
        render();
        if (!hasPerm() && sel == -1) requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    @Override protected void onResume() { super.onResume(); start(); }
    @Override protected void onPause() { super.onPause(); stop(); }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        if (themeMode == 0 && resolvePal().dark != P.dark) recreate();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        start();
        render();
    }

    private boolean hasPerm() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void start() {
        if (running) return;
        running = true;
        handler.post(ticker);
        if (hasPerm() && sel == -1) requestLocation();
    }

    private void stop() {
        handler.removeCallbacks(ticker);
        try { lm.removeUpdates(locListener); } catch (Exception e) { }
        running = false;
    }

    private Pal resolvePal() {
        if (themeMode == 1) return Pal.light();
        if (themeMode == 2) return Pal.dark();
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return night ? Pal.dark() : Pal.light();
    }

    // ------------------------------------------------------------- location
    private void requestLocation() {
        try {
            Location a = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location b = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location best = a != null && (b == null || a.getTime() >= b.getTime()) ? a : b;
            if (best != null) onGps(best.getLatitude(), best.getLongitude());
            lm.removeUpdates(locListener);
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 120000, 2000, locListener);
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 120000, 2000, locListener);
        } catch (SecurityException | IllegalArgumentException e) { /* shown as "waiting for location" */ }
    }

    private void onGps(double la, double lo) {
        boolean first = gps == null;
        boolean moved = first || Math.hypot((la - gps.lat) * 111.32, (lo - gps.lon) * 111.32 * Math.cos(Math.toRadians(la))) > 5;
        String name = gps == null ? "" : gps.name;
        gps = new Place(moved ? "" : name, la, lo);
        if (!moved) { gps.name = name; return; }
        prefs.edit().putLong("glat", Double.doubleToLongBits(la)).putLong("glon", Double.doubleToLongBits(lo)).putString("gname", "").apply();
        geocode(la, lo);
        if (sel == -1) loadWeather(true);
    }

    private void geocode(final double la, final double lo) {
        if (geoBusy) return;
        geoBusy = true;
        final Locale loc = new Locale(fmt.lang == Lang.UR ? "ur" : (fmt.lang == Lang.AR ? "ar" : "en"));
        io.execute(new Runnable() {
            @Override public void run() {
                String n = "";
                try {
                    if (Geocoder.isPresent()) {
                        List<Address> l = new Geocoder(MainActivity.this, loc).getFromLocation(la, lo, 1);
                        if (l != null && !l.isEmpty()) {
                            Address ad = l.get(0);
                            String city = ad.getLocality() != null ? ad.getLocality() : (ad.getSubAdminArea() != null ? ad.getSubAdminArea() : ad.getAdminArea());
                            n = (city != null ? city : "") + (ad.getCountryName() != null ? (city != null ? ", " : "") + ad.getCountryName() : "");
                        }
                    }
                } catch (Exception e) { /* name stays "My location" */ }
                final String name = n;
                geoBusy = false;
                handler.post(new Runnable() {
                    @Override public void run() {
                        if (name.length() > 0 && gps != null) {
                            gps.name = name;
                            prefs.edit().putString("gname", name).apply();
                            render();
                        }
                    }
                });
            }
        });
    }

    private Place currentPlace() {
        if (sel >= 0 && sel < saved.size()) return saved.get(sel);
        return gps;
    }

    private String placeTitle(Place p) {
        if (p == null) return "";
        if (p == gps) return "\uD83D\uDCCD " + (p.name.length() > 0 ? p.name : t("my_loc"));
        return p.name;
    }

    // ------------------------------------------------------------- saved places
    private void loadPlaces() {
        saved.clear();
        try {
            JSONArray a = new JSONArray(prefs.getString("places", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                saved.add(new Place(o.getString("n"), o.getDouble("la"), o.getDouble("lo")));
            }
        } catch (Exception e) { saved.clear(); }
    }

    private void savePlaces() {
        try {
            JSONArray a = new JSONArray();
            for (Place p : saved) {
                JSONObject o = new JSONObject();
                o.put("n", p.name);
                o.put("la", p.lat);
                o.put("lo", p.lon);
                a.put(o);
            }
            prefs.edit().putString("places", a.toString()).putInt("sel", sel).apply();
        } catch (Exception e) { /* ignore */ }
    }

    private void selectPlace(int index) {
        sel = index;
        prefs.edit().putInt("sel", sel).apply();
        wx = null;
        Place pl = currentPlace();
        if (pl != null) { wxKey = pl.key(); wx = readCache(wxKey); }
        if (sel == -1 && hasPerm()) requestLocation();
        tab = 0;
        prefs.edit().putInt("tab", 0).apply();
        showTab();
        render();
        loadWeather(true);
    }

    private void addPlace(Place p) {
        for (int i = 0; i < saved.size(); i++) {
            Place q = saved.get(i);
            if (Math.abs(q.lat - p.lat) < 0.05 && Math.abs(q.lon - p.lon) < 0.05) { selectPlace(i); return; }
        }
        saved.add(p);
        sel = saved.size() - 1;
        savePlaces();
        selectPlace(sel);
    }

    // ------------------------------------------------------------- network
    private static String http(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent", "Weather-BlackFalcon/1.0");
            if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
            InputStream in = c.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }

    private static String forecastUrl(double la, double lo) {
        return String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m,dew_point_2m"
                + "&hourly=temperature_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,uv_index,visibility,is_day"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_sum,precipitation_probability_max,wind_speed_10m_max"
                + "&timezone=auto&forecast_days=7", la, lo);
    }

    private static String airUrl(double la, double lo) {
        return String.format(Locale.US, "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=%.4f&longitude=%.4f&current=us_aqi,pm2_5,pm10&timezone=auto", la, lo);
    }

    private Wx readCache(String key) {
        try {
            if (!key.equals(prefs.getString("c_key", ""))) return null;
            String fc = prefs.getString("c_fc", null);
            if (fc == null) return null;
            Wx w = Wx.parse(fc, prefs.getString("c_aq", null));
            w.updatedMs = prefs.getLong("c_t", 0);
            return w;
        } catch (Exception e) {
            return null;
        }
    }

    private void loadWeather(final boolean force) {
        final Place pl = currentPlace();
        if (pl == null) { render(); return; }
        final String key = pl.key();
        if (wx == null || !key.equals(wxKey)) { wx = readCache(key); wxKey = key; }
        long now = System.currentTimeMillis();
        if (fetching || (!force && wx != null && now - wx.updatedMs < FRESH_MS)) { render(); return; }
        fetching = true;
        render();
        io.execute(new Runnable() {
            @Override public void run() {
                String fc = null, aq = null;
                Wx nw = null;
                try {
                    fc = http(forecastUrl(pl.lat, pl.lon));
                    try { aq = http(airUrl(pl.lat, pl.lon)); } catch (Exception e) { aq = null; }
                    nw = Wx.parse(fc, aq);
                    nw.updatedMs = System.currentTimeMillis();
                } catch (Exception e) {
                    nw = null;
                }
                final Wx fw = nw;
                final String ffc = fc, faq = aq;
                handler.post(new Runnable() {
                    @Override public void run() {
                        fetching = false;
                        Place cur = currentPlace();
                        boolean same = cur != null && cur.key().equals(key);
                        if (fw != null) {
                            prefs.edit().putString("c_key", key).putString("c_fc", ffc).putString("c_aq", faq).putLong("c_t", fw.updatedMs).apply();
                            if (same) { wx = fw; wxKey = key; offline = false; }
                        } else if (same) offline = true;
                        render();
                        if (!same) loadWeather(true);
                    }
                });
            }
        });
    }

    private void doSearch() {
        final String q = searchEt.getText().toString().trim();
        if (q.length() < 2 || searching) return;
        searching = true;
        searchMsg = t("loading");
        refreshPlaces();
        final String lc = fmt.lang == Lang.UR ? "ur" : (fmt.lang == Lang.AR ? "ar" : "en");
        io.execute(new Runnable() {
            @Override public void run() {
                final List<Place> found = new ArrayList<>();
                boolean ok = true;
                try {
                    String body = http("https://geocoding-api.open-meteo.com/v1/search?name=" + URLEncoder.encode(q, "UTF-8") + "&count=8&format=json&language=" + lc);
                    JSONArray a = new JSONObject(body).optJSONArray("results");
                    if (a != null) {
                        for (int i = 0; i < a.length(); i++) {
                            JSONObject o = a.getJSONObject(i);
                            String n = o.optString("name", "");
                            String adm = o.optString("admin1", "");
                            String co = o.optString("country", "");
                            String full = n + (adm.length() > 0 && !adm.equals(n) ? ", " + adm : "") + (co.length() > 0 ? ", " + co : "");
                            found.add(new Place(full, o.getDouble("latitude"), o.getDouble("longitude")));
                        }
                    }
                } catch (Exception e) {
                    ok = false;
                }
                final boolean fok = ok;
                handler.post(new Runnable() {
                    @Override public void run() {
                        searching = false;
                        results = found;
                        searchMsg = !fok ? t("no_data") : (found.isEmpty() ? t("no_results") : "");
                        refreshPlaces();
                    }
                });
            }
        });
    }

    // ------------------------------------------------------------- view helpers
    private GradientDrawable box(int fill, int edge, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setStroke(dp(1), edge);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private TextView tv(String text, int size, int color) {
        TextView x = new TextView(this);
        x.setText(text);
        x.setTextSize(size);
        x.setTextColor(color);
        return x;
    }

    private LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(box(P.card, P.edge, 16));
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams l = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        l.bottomMargin = dp(10);
        c.setLayoutParams(l);
        return c;
    }

    private TextView title(String s) {
        TextView x = tv(s, 13, P.sub);
        x.setTypeface(Typeface.DEFAULT_BOLD);
        x.setPadding(0, 0, 0, dp(8));
        return x;
    }

    private IconView icon(int code, boolean day, int sizeDp) {
        IconView v = new IconView(this);
        v.set(code, day, P.cloud, P.rain);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return v;
    }

    private TextView chip(String text, boolean selected, View.OnClickListener l) {
        TextView c = tv(text, 14, selected ? P.text : P.sub);
        c.setGravity(Gravity.CENTER);
        c.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        c.setBackground(box(selected ? P.bg : P.card, selected ? P.accent : P.edge, 10));
        c.setPadding(dp(8), dp(10), dp(8), dp(10));
        c.setOnClickListener(l);
        return c;
    }

    private TextView button(String text) {
        TextView b = tv(text, 15, P.text);
        b.setGravity(Gravity.CENTER);
        b.setBackground(box(P.card, P.accent, 12));
        b.setPadding(dp(12), dp(11), dp(12), dp(11));
        LinearLayout.LayoutParams l = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        l.topMargin = dp(8);
        b.setLayoutParams(l);
        return b;
    }

    private LinearLayout.LayoutParams weight(int left, int right) {
        LinearLayout.LayoutParams l = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        l.setMargins(left, 0, right, 0);
        return l;
    }

    // ------------------------------------------------------------- UI
    private void buildUi() {
        Window w = getWindow();
        w.setStatusBarColor(P.bg);
        w.setNavigationBarColor(P.bg);
        int flags = 0;
        if (!P.dark) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        w.getDecorView().setSystemUiVisibility(flags);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(P.bg);
        root.setLayoutDirection(fmt.lang == Lang.EN ? View.LAYOUT_DIRECTION_LTR : View.LAYOUT_DIRECTION_RTL);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(10), dp(6), dp(10), dp(4));

        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setPadding(0, 0, 0, dp(8));
        String[] keys = {"tab_wx", "tab_places", "tab_set"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView x = tv(t(keys[i]), 14, P.text);
            x.setGravity(Gravity.CENTER);
            x.setPadding(0, dp(9), 0, dp(9));
            x.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { tab = idx; prefs.edit().putInt("tab", tab).apply(); showTab(); } });
            tabRow.addView(x, weight(dp(2), dp(2)));
            tabs[i] = x;
        }
        col.addView(tabRow);

        FrameLayout frame = new FrameLayout(this);
        wxScroll = new ScrollView(this);
        wxScroll.setVerticalScrollBarEnabled(false);
        wxBox = new LinearLayout(this);
        wxBox.setOrientation(LinearLayout.VERTICAL);
        wxBox.setPadding(dp(2), dp(2), dp(2), dp(48));
        wxScroll.addView(wxBox);
        pages[0] = wxScroll;

        placesScroll = new ScrollView(this);
        placesBox = new LinearLayout(this);
        placesBox.setOrientation(LinearLayout.VERTICAL);
        placesBox.setPadding(dp(2), dp(2), dp(2), dp(48));
        placesScroll.addView(placesBox);
        pages[1] = placesScroll;
        pages[2] = buildSettings();
        for (View v : pages) frame.addView(v);
        col.addView(frame, lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView credit = tv("By: Black Falcon \uD83E\uDD85", 12, P.sub);
        credit.setPadding(dp(8), dp(3), dp(8), dp(3));
        credit.setBackground(box(P.card, P.edge, 8));
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.RIGHT);
        cl.setMargins(0, 0, dp(8), dp(6));
        root.addView(credit, cl);

        setContentView(root);
        buildPlaces();
        showTab();
    }

    private void showTab() {
        for (int i = 0; i < 3; i++) {
            pages[i].setVisibility(i == tab ? View.VISIBLE : View.GONE);
            tabs[i].setBackground(box(i == tab ? P.card : P.bg, i == tab ? P.accent : P.edge, 10));
            tabs[i].setTypeface(i == tab ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
        if (tab == 1) refreshPlaces();
    }

    // ------------------------------------------------------------- weather page
    private void render() {
        if (wxBox == null) return;
        int sy = wxScroll.getScrollY();
        wxBox.removeAllViews();
        Place pl = currentPlace();

        // header
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout hl = new LinearLayout(this);
        hl.setOrientation(LinearLayout.VERTICAL);
        TextView name = tv(pl == null ? t("my_loc") : placeTitle(pl), 20, P.text);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        hl.addView(name);
        String status;
        int sc = P.sub;
        if (fetching) status = t("loading");
        else if (wx != null && offline) { status = t("offline") + " " + fmt.deviceClock(wx.updatedMs); sc = 0xFFFB8C00; }
        else if (wx != null) status = t("updated") + " " + fmt.deviceClock(wx.updatedMs);
        else status = "";
        TextView st = tv(status, 12, sc);
        hl.addView(st);
        head.addView(hl, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView refresh = tv("\u27F3", 24, P.accent);
        refresh.setGravity(Gravity.CENTER);
        refresh.setPadding(dp(12), dp(4), dp(12), dp(4));
        refresh.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { loadWeather(true); } });
        head.addView(refresh);
        LinearLayout.LayoutParams hp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        hp.bottomMargin = dp(8);
        wxBox.addView(head, hp);

        if (wx == null) {
            LinearLayout c = card();
            String m = pl == null ? (hasPerm() ? t("wait_loc") : t("need_perm")) : (fetching ? t("loading") : t("no_data"));
            c.addView(tv(m, 15, P.text));
            wxBox.addView(c);
        } else {
            int i0 = wx.nowIndex(System.currentTimeMillis());
            wxBox.addView(heroCard(i0));
            wxBox.addView(hourlyCard(i0));
            wxBox.addView(chartCard(i0));
            wxBox.addView(dailyCard());
            addDetails(i0);
        }
        TextView src = tv(t("source"), 11, P.sub);
        src.setPadding(dp(4), dp(4), dp(4), dp(8));
        wxBox.addView(src);
        final int fy = sy;
        wxScroll.post(new Runnable() { @Override public void run() { wxScroll.scrollTo(0, fy); } });
    }

    private View heroCard(int i0) {
        int[] g = WxIcon.gradient(wx.code, wx.isDay);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, g);
        bg.setCornerRadius(dp(22));
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setBackground(bg);
        hero.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams hp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        hp.bottomMargin = dp(10);
        hero.setLayoutParams(hp);

        hero.addView(tv(fmt.dateLine(wx.curTime > 0 ? wx.curTime : System.currentTimeMillis(), wx.tz), 14, 0xCCFFFFFF));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        TextView temp = tv(fmt.temp(wx.temp), 64, Color.WHITE);
        temp.setTypeface(Typeface.DEFAULT_BOLD);
        temp.setIncludeFontPadding(false);
        left.addView(temp);
        TextView cond = tv(t(WxIcon.key(wx.code)), 18, Color.WHITE);
        left.addView(cond);
        left.addView(tv(t("feels") + " " + fmt.temp(wx.feels), 14, 0xDDFFFFFF));
        row.addView(left, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        IconView ic = new IconView(this);
        ic.set(wx.code, wx.isDay, 0xFFF1F5FB, 0xFF81D4FA);
        row.addView(ic, new LinearLayout.LayoutParams(dp(110), dp(110)));
        hero.addView(row);

        String hl = "";
        if (wx.dn > 0) hl = t("hi") + " " + fmt.temp(wx.dMax[0]) + "   " + t("lo") + " " + fmt.temp(wx.dMin[0]);
        TextView hlv = tv(hl, 15, Color.WHITE);
        hlv.setTypeface(Typeface.DEFAULT_BOLD);
        hlv.setPadding(0, dp(6), 0, dp(6));
        hero.addView(hlv);
        TextView out = tv(fmt.outlook(wx, i0), 14, Color.WHITE);
        out.setBackground(box(0x33FFFFFF, 0x00000000, 12));
        out.setPadding(dp(12), dp(7), dp(12), dp(7));
        hero.addView(out);
        return hero;
    }

    private View hourlyCard(int i0) {
        LinearLayout c = card();
        c.addView(title(t("next24")));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        LinearLayout strip = new LinearLayout(this);
        strip.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        int n = Math.min(24, wx.hn - i0);
        for (int k = 0; k < n; k++) {
            int i = i0 + k;
            LinearLayout it = new LinearLayout(this);
            it.setOrientation(LinearLayout.VERTICAL);
            it.setGravity(Gravity.CENTER_HORIZONTAL);
            it.setPadding(dp(4), 0, dp(4), 0);
            TextView tm = tv(k == 0 ? t("now") : fmt.hour(wx.hTime[i], wx.tz), 12, P.sub);
            tm.setGravity(Gravity.CENTER);
            it.addView(tm);
            LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(dp(38), dp(38));
            il.topMargin = dp(4);
            IconView ic = icon(wx.hCode[i], wx.hDay[i], 38);
            ic.setLayoutParams(il);
            it.addView(ic);
            TextView tp = tv(fmt.temp(wx.hTemp[i]), 15, P.text);
            tp.setTypeface(Typeface.DEFAULT_BOLD);
            it.addView(tp);
            double pop = wx.hPop[i];
            TextView pv = tv(!Double.isNaN(pop) && pop >= 10 ? Math.round(pop) + "%" : " ", 11, P.rain);
            it.addView(pv);
            strip.addView(it, new LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        hs.addView(strip);
        c.addView(hs);
        return c;
    }

    private View chartCard(int i0) {
        LinearLayout c = card();
        c.addView(title(t("chart")));
        int n = Math.min(24, wx.hn - i0);
        double[] temp = new double[n], pop = new double[n];
        String[] tl = new String[n], xl = new String[n];
        for (int k = 0; k < n; k++) {
            temp[k] = fmt.imperial && !Double.isNaN(wx.hTemp[i0 + k]) ? wx.hTemp[i0 + k] * 9.0 / 5.0 + 32.0 : wx.hTemp[i0 + k];
            pop[k] = wx.hPop[i0 + k];
            tl[k] = Double.isNaN(temp[k]) ? "" : String.valueOf(Math.round(temp[k]));
            xl[k] = k % 3 == 0 ? fmt.hour(wx.hTime[i0 + k], wx.tz) : "";
        }
        ChartView cv = new ChartView(this);
        cv.set(temp, pop, tl, xl, 0xFFFFA000, P.dark ? 0x554FC3F7 : 0x551E88E5, P.sub, P.dark ? 0x22FFFFFF : 0x22000000);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        hs.addView(cv, new FrameLayout.LayoutParams(dp(40) * Math.max(n, 2) + dp(20), dp(170)));
        c.addView(hs);
        return c;
    }

    private View dailyCard() {
        LinearLayout c = card();
        c.addView(title(t("days7")));
        double mn = 1e9, mx = -1e9;
        for (int i = 0; i < wx.dn; i++) {
            if (!Double.isNaN(wx.dMin[i])) mn = Math.min(mn, wx.dMin[i]);
            if (!Double.isNaN(wx.dMax[i])) mx = Math.max(mx, wx.dMax[i]);
        }
        long now = wx.curTime > 0 ? wx.curTime : System.currentTimeMillis();
        for (int i = 0; i < wx.dn; i++) {
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(0, dp(5), 0, dp(5));
            String day;
            int diff = Fmt.dayKey(wx.dTime[i], wx.tz) - Fmt.dayKey(now, wx.tz);
            if (i == 0 && diff == 0) day = t("today");
            else if (i == 1) day = t("tomorrow");
            else day = fmt.weekday(wx.dTime[i], wx.tz);
            r.addView(tv(day, 14, P.text), lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.15f));
            r.addView(icon(wx.dCode[i], true, 30));
            double pop = wx.dPop[i];
            TextView pv = tv(!Double.isNaN(pop) && pop >= 10 ? Math.round(pop) + "%" : "", 11, P.rain);
            pv.setGravity(Gravity.CENTER);
            r.addView(pv, new LinearLayout.LayoutParams(dp(36), ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView lo = tv(fmt.temp(wx.dMin[i]), 14, P.sub);
            lo.setGravity(Gravity.CENTER);
            r.addView(lo, new LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT));
            RangeBar rb = new RangeBar(this);
            rb.set((float) wx.dMin[i], (float) wx.dMax[i], (float) mn, (float) mx, P.dark ? 0xFF2A3350 : 0xFFE3E8F3, 0xFF4FC3F7, 0xFFFFA000);
            r.addView(rb, lp(0, dp(16), 1.3f));
            TextView hi = tv(fmt.temp(wx.dMax[i]), 14, P.text);
            hi.setGravity(Gravity.CENTER);
            hi.setTypeface(Typeface.DEFAULT_BOLD);
            r.addView(hi, new LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT));
            c.addView(r);
        }
        return c;
    }

    private LinearLayout detail(String ttl, String value, String extra, int valueColor, View right) {
        LinearLayout c = new LinearLayout(this);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setBackground(box(P.card, P.edge, 16));
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(tv(ttl, 12, P.sub));
        TextView v = tv(value, 20, valueColor);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(v);
        if (extra != null && extra.length() > 0) col.addView(tv(extra, 12, P.sub));
        c.addView(col, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (right != null) c.addView(right);
        return c;
    }

    private void addRow(View a, View b) {
        LinearLayout r = new LinearLayout(this);
        r.setBaselineAligned(false);
        LinearLayout.LayoutParams la = lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        la.rightMargin = dp(5);
        LinearLayout.LayoutParams lb = lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lb.leftMargin = dp(5);
        r.addView(a, la);
        if (b != null) r.addView(b, lb);
        LinearLayout.LayoutParams rp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        rp.bottomMargin = dp(10);
        wxBox.addView(r, rp);
    }

    private void addDetails(int i0) {
        ArrowView arrow = new ArrowView(this);
        arrow.set(Double.isNaN(wx.windDir) ? 0 : (float) ((wx.windDir + 180) % 360), P.sub, P.accent);
        arrow.setLayoutParams(new LinearLayout.LayoutParams(dp(46), dp(46)));
        String windExtra = fmt.dir(wx.windDir) + (Double.isNaN(wx.gust) ? "" : "  \u00B7  " + t("gusts") + " " + fmt.windNum(wx.gust));
        addRow(detail(t("wind"), fmt.wind(wx.wind), windExtra, P.text, arrow),
                detail(t("humidity"), fmt.pct(wx.hum), t("dew") + " " + fmt.temp(wx.dew), P.text, null));

        double uv = i0 < wx.hn && !Double.isNaN(wx.hUv[i0]) ? wx.hUv[i0] : (wx.dn > 0 ? wx.dUv[0] : Double.NaN);
        String uvTxt = Double.isNaN(uv) ? "--" : String.format(Locale.US, "%.0f", uv);
        int uvc = Double.isNaN(uv) ? P.text : Fmt.UV_COLOR[Fmt.uvCat(uv)];
        String uvExtra = Double.isNaN(uv) ? "" : t("uv" + Fmt.uvCat(uv)) + (wx.dn > 0 && !Double.isNaN(wx.dUv[0]) ? "  \u00B7  " + t("hi") + " " + String.format(Locale.US, "%.0f", wx.dUv[0]) : "");
        addRow(detail(t("uv"), uvTxt, uvExtra, uvc, null),
                detail(t("pressure"), fmt.press(wx.press), "", P.text, null));

        double vis = i0 < wx.hn ? wx.hVis[i0] : Double.NaN;
        addRow(detail(t("vis"), fmt.vis(vis), "", P.text, null),
                detail(t("cloud"), fmt.pct(wx.cloud), "", P.text, null));

        String pExtra = wx.dn > 0 ? fmt.precip(wx.dPrec[0]) + (Double.isNaN(wx.dPop[0]) ? "" : "  \u00B7  " + Math.round(wx.dPop[0]) + "% " + t("chance")) : "";
        addRow(detail(t("precip"), fmt.precip(wx.precip), pExtra, P.text, null), buildAqi());

        if (wx.dn > 0 && wx.dRise[0] > 0 && wx.dSet[0] > 0) {
            LinearLayout c = card();
            LinearLayout r = new LinearLayout(this);
            LinearLayout a = new LinearLayout(this);
            a.setOrientation(LinearLayout.VERTICAL);
            a.addView(tv("\u2600 " + t("sunrise"), 12, P.sub));
            TextView at = tv(fmt.clock(wx.dRise[0], wx.tz), 20, P.text);
            at.setTypeface(Typeface.DEFAULT_BOLD);
            a.addView(at);
            LinearLayout b = new LinearLayout(this);
            b.setOrientation(LinearLayout.VERTICAL);
            b.addView(tv("\u263D " + t("sunset"), 12, P.sub));
            TextView bt = tv(fmt.clock(wx.dSet[0], wx.tz), 20, P.text);
            bt.setTypeface(Typeface.DEFAULT_BOLD);
            b.addView(bt);
            LinearLayout d = new LinearLayout(this);
            d.setOrientation(LinearLayout.VERTICAL);
            d.addView(tv(t("daylen"), 12, P.sub));
            TextView dt = tv(fmt.duration(wx.dSet[0] - wx.dRise[0]), 20, P.text);
            dt.setTypeface(Typeface.DEFAULT_BOLD);
            d.addView(dt);
            r.addView(a, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            r.addView(b, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            r.addView(d, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            c.addView(r);
            wxBox.addView(c);
        }
    }

    private View buildAqi() {
        if (!wx.hasAir) return detail(t("aqi"), "--", "", P.text, null);
        int cat = Fmt.aqiCat(wx.aqi);
        String extra = t("aq" + cat);
        if (!Double.isNaN(wx.pm25)) extra += "\nPM2.5 " + Math.round(wx.pm25) + (Double.isNaN(wx.pm10) ? "" : "  \u00B7  PM10 " + Math.round(wx.pm10));
        return detail(t("aqi"), String.valueOf(Math.round(wx.aqi)), extra, Fmt.AQI_COLOR[cat], null);
    }

    // ------------------------------------------------------------- places page
    private void buildPlaces() {
        placesBox.removeAllViews();
        TextView gpsBtn = button("\uD83D\uDCCD  " + t("use_gps"));
        gpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!hasPerm()) requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                selectPlace(-1);
            }
        });
        LinearLayout.LayoutParams gl = (LinearLayout.LayoutParams) gpsBtn.getLayoutParams();
        gl.topMargin = 0;
        placesBox.addView(gpsBtn);

        LinearLayout sr = new LinearLayout(this);
        sr.setGravity(Gravity.CENTER_VERTICAL);
        searchEt = new EditText(this);
        searchEt.setHint(t("search_hint"));
        searchEt.setHintTextColor(P.sub);
        searchEt.setTextColor(P.text);
        searchEt.setSingleLine(true);
        searchEt.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent e) { doSearch(); return true; }
        });
        sr.addView(searchEt, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView go = tv(t("search"), 15, P.accent);
        go.setTypeface(Typeface.DEFAULT_BOLD);
        go.setPadding(dp(14), dp(10), dp(14), dp(10));
        go.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { doSearch(); } });
        sr.addView(go);
        LinearLayout.LayoutParams srp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        srp.topMargin = dp(10);
        placesBox.addView(sr, srp);

        LinearLayout dyn = new LinearLayout(this);
        dyn.setOrientation(LinearLayout.VERTICAL);
        dyn.setTag("dyn");
        placesBox.addView(dyn);
        refreshPlaces();
    }

    private void refreshPlaces() {
        if (placesBox == null) return;
        LinearLayout dyn = (LinearLayout) placesBox.findViewWithTag("dyn");
        if (dyn == null) return;
        dyn.removeAllViews();
        if (searchMsg.length() > 0) {
            TextView m = tv(searchMsg, 14, P.sub);
            m.setPadding(dp(4), dp(10), dp(4), 0);
            dyn.addView(m);
        }
        if (!results.isEmpty()) {
            TextView h = title(t("results"));
            h.setPadding(dp(4), dp(14), 0, dp(6));
            dyn.addView(h);
            for (final Place p : results) {
                TextView row = tv("+  " + p.name, 15, P.text);
                row.setBackground(box(P.card, P.edge, 12));
                row.setPadding(dp(14), dp(12), dp(14), dp(12));
                LinearLayout.LayoutParams l = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
                l.bottomMargin = dp(6);
                row.setLayoutParams(l);
                row.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        results = new ArrayList<>();
                        searchMsg = "";
                        searchEt.setText("");
                        addPlace(p);
                    }
                });
                dyn.addView(row);
            }
        }
        if (!saved.isEmpty()) {
            TextView h = title(t("saved"));
            h.setPadding(dp(4), dp(14), 0, dp(6));
            dyn.addView(h);
            for (int i = 0; i < saved.size(); i++) {
                final int idx = i;
                LinearLayout row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackground(box(P.card, sel == i ? P.accent : P.edge, 12));
                row.setPadding(dp(14), dp(4), dp(4), dp(4));
                LinearLayout.LayoutParams l = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
                l.bottomMargin = dp(6);
                row.setLayoutParams(l);
                TextView n = tv(saved.get(i).name, 15, P.text);
                n.setTypeface(sel == i ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                n.setPadding(0, dp(10), 0, dp(10));
                n.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { selectPlace(idx); } });
                row.addView(n, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                TextView del = tv("\u2715", 18, P.sub);
                del.setPadding(dp(14), dp(10), dp(14), dp(10));
                del.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        boolean wasSelected = sel == idx;
                        saved.remove(idx);
                        if (wasSelected) sel = -1;
                        else if (sel > idx) sel--;
                        savePlaces();
                        if (wasSelected) selectPlace(-1);
                        else refreshPlaces();
                    }
                });
                row.addView(del);
                dyn.addView(row);
            }
        }
    }

    // ------------------------------------------------------------- settings page
    private View buildSettings() {
        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(2), dp(2), dp(2), dp(48));
        sv.addView(box);

        box.addView(title(t("lang")));
        LinearLayout lr = new LinearLayout(this);
        String[] ln = {"English", "اردو", "العربية"};
        for (int i = 0; i < 3; i++) {
            final int li = i;
            lr.addView(chip(ln[i], fmt.lang == i, new View.OnClickListener() {
                @Override public void onClick(View v) { prefs.edit().putInt("lang", li).apply(); recreate(); }
            }), weight(dp(2), dp(2)));
        }
        LinearLayout.LayoutParams lrp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        lrp.bottomMargin = dp(16);
        box.addView(lr, lrp);

        box.addView(title(t("theme")));
        LinearLayout tr = new LinearLayout(this);
        String[] tk = {"th_auto", "th_light", "th_dark"};
        for (int i = 0; i < 3; i++) {
            final int ti = i;
            tr.addView(chip(t(tk[i]), themeMode == i, new View.OnClickListener() {
                @Override public void onClick(View v) { prefs.edit().putInt("theme", ti).apply(); recreate(); }
            }), weight(dp(2), dp(2)));
        }
        LinearLayout.LayoutParams trp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        trp.bottomMargin = dp(16);
        box.addView(tr, trp);

        box.addView(title(t("units")));
        LinearLayout ur = new LinearLayout(this);
        ur.addView(chip(t("metric"), !fmt.imperial, new View.OnClickListener() {
            @Override public void onClick(View v) { prefs.edit().putBoolean("imperial", false).apply(); recreate(); }
        }), weight(dp(2), dp(2)));
        ur.addView(chip(t("imperial"), fmt.imperial, new View.OnClickListener() {
            @Override public void onClick(View v) { prefs.edit().putBoolean("imperial", true).apply(); recreate(); }
        }), weight(dp(2), dp(2)));
        box.addView(ur);
        TextView un = tv(t("units_note"), 12, P.sub);
        un.setPadding(dp(4), dp(6), dp(4), dp(16));
        box.addView(un);

        box.addView(title(t("tf")));
        LinearLayout fr = new LinearLayout(this);
        fr.addView(chip(t("h12"), !fmt.h24, new View.OnClickListener() {
            @Override public void onClick(View v) { prefs.edit().putBoolean("h24", false).apply(); recreate(); }
        }), weight(dp(2), dp(2)));
        fr.addView(chip(t("h24"), fmt.h24, new View.OnClickListener() {
            @Override public void onClick(View v) { prefs.edit().putBoolean("h24", true).apply(); recreate(); }
        }), weight(dp(2), dp(2)));
        box.addView(fr);

        TextView src = tv(t("source"), 11, P.sub);
        src.setPadding(dp(4), dp(18), dp(4), 0);
        box.addView(src);
        TextView credit = tv("Brought to you By: Black Falcon \uD83E\uDD85", 13, P.sub);
        credit.setPadding(dp(4), dp(12), 0, 0);
        box.addView(credit);
        return sv;
    }
}
