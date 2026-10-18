package com.blackfalcon.weather;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Small dial with an arrow showing where the wind is blowing to. */
public class ArrowView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float dir = 0;      // direction the arrow points to, degrees clockwise from north
    private int ring = 0xFF888888, arrow = 0xFF4FC3F7;

    public ArrowView(Context c) { super(c); }

    public void set(float toDeg, int ring, int arrow) {
        dir = toDeg; this.ring = ring; this.arrow = arrow;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas cv) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy) - 2;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(r * 0.08f);
        p.setColor(ring);
        cv.drawCircle(cx, cy, r, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(ring);
        cv.drawCircle(cx, cy - r, r * 0.09f, p);          // north mark
        cv.save();
        cv.rotate(dir, cx, cy);
        Path a = new Path();
        a.moveTo(cx, cy - r * 0.72f);
        a.lineTo(cx + r * 0.28f, cy - r * 0.2f);
        a.lineTo(cx + r * 0.09f, cy - r * 0.2f);
        a.lineTo(cx + r * 0.09f, cy + r * 0.62f);
        a.lineTo(cx - r * 0.09f, cy + r * 0.62f);
        a.lineTo(cx - r * 0.09f, cy - r * 0.2f);
        a.lineTo(cx - r * 0.28f, cy - r * 0.2f);
        a.close();
        p.setColor(arrow);
        cv.drawPath(a, p);
        cv.restore();
    }
}
