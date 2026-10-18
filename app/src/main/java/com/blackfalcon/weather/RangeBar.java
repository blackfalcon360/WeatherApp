package com.blackfalcon.weather;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** A day's low-to-high temperature drawn as a bar inside the week's overall range. */
public class RangeBar extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float lo, hi, min, max;
    private int track = 0xFFCCCCCC, fill = 0xFF4FC3F7, fill2 = 0xFFFFB300;

    public RangeBar(Context c) { super(c); }

    public void set(float lo, float hi, float min, float max, int track, int cold, int warm) {
        this.lo = lo; this.hi = hi; this.min = min; this.max = max;
        this.track = track; this.fill = cold; this.fill2 = warm;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight(), th = h * 0.34f, y = (h - th) / 2f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(track);
        cv.drawRoundRect(new RectF(0, y, w, y + th), th / 2, th / 2, p);
        float span = Math.max(0.1f, max - min);
        float x0 = w * (lo - min) / span, x1 = w * (hi - min) / span;
        if (x1 - x0 < th) x1 = x0 + th;
        p.setShader(new android.graphics.LinearGradient(x0, 0, x1, 0, fill, fill2, android.graphics.Shader.TileMode.CLAMP));
        cv.drawRoundRect(new RectF(x0, y, x1, y + th), th / 2, th / 2, p);
        p.setShader(null);
    }
}
