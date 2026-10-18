package com.blackfalcon.weather;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** 24-hour temperature curve with rain-chance bars underneath. */
public class ChartView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private double[] temp = new double[0], pop = new double[0];
    private String[] tl = new String[0], xl = new String[0];
    private int line = 0xFFFFB300, bar = 0x554FC3F7, text = 0xFF888888, grid = 0x22888888;

    public ChartView(Context c) { super(c); }

    public void set(double[] temp, double[] pop, String[] tempLabels, String[] timeLabels, int line, int bar, int text, int grid) {
        this.temp = temp; this.pop = pop; this.tl = tempLabels; this.xl = timeLabels;
        this.line = line; this.bar = bar; this.text = text; this.grid = grid;
        invalidate();
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    @Override
    protected void onDraw(Canvas cv) {
        int n = temp.length;
        if (n < 2) return;
        float w = getWidth(), h = getHeight();
        float left = dp(20), right = dp(20), top = dp(26), bottom = dp(24);
        float cw = w - left - right, ch = h - top - bottom;
        double mn = 1e9, mx = -1e9;
        for (double t : temp) { if (Double.isNaN(t)) continue; mn = Math.min(mn, t); mx = Math.max(mx, t); }
        if (mn > mx) return;
        if (mx - mn < 2) { mx += 1; mn -= 1; }
        float step = cw / (n - 1);
        // rain-chance bars
        p.setStyle(Paint.Style.FILL);
        p.setColor(bar);
        for (int i = 0; i < n; i++) {
            double v = Double.isNaN(pop[i]) ? 0 : pop[i];
            float bh = (float) (v / 100.0 * ch * 0.45);
            float x = left + i * step;
            cv.drawRect(x - dp(6), top + ch - bh, x + dp(6), top + ch, p);
        }
        // grid
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1));
        p.setColor(grid);
        for (int g = 0; g < 3; g++) cv.drawLine(left, top + ch * g / 2f, w - right, top + ch * g / 2f, p);
        // curve
        Path path = new Path();
        float[] xs = new float[n], ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = left + i * step;
            double t = Double.isNaN(temp[i]) ? mn : temp[i];
            ys[i] = (float) (top + (1 - (t - mn) / (mx - mn)) * (ch * 0.78) + ch * 0.02);
        }
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            float mx2 = (xs[i - 1] + xs[i]) / 2f;
            path.cubicTo(mx2, ys[i - 1], mx2, ys[i], xs[i], ys[i]);
        }
        p.setStrokeWidth(dp(3));
        p.setColor(line);
        cv.drawPath(path, p);
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < n; i++) {
            cv.drawCircle(xs[i], ys[i], dp(3.5f), p);
            p.setColor(text);
            p.setTextSize(dp(11));
            cv.drawText(tl[i], xs[i], ys[i] - dp(8), p);
            p.setTextSize(dp(10));
            cv.drawText(xl[i], xs[i], h - dp(6), p);
            p.setColor(line);
        }
    }
}
