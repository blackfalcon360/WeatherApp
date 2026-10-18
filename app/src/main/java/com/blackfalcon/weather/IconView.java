package com.blackfalcon.weather;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class IconView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int code = 0, cloud = 0xFFD0DAEA, rain = 0xFF4FC3F7;
    private boolean day = true;

    public IconView(Context c) { super(c); }

    public void set(int code, boolean day, int cloud, int rain) {
        this.code = code; this.day = day; this.cloud = cloud; this.rain = rain;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas cv) {
        WxIcon.draw(cv, p, getWidth() / 2f, getHeight() / 2f, Math.min(getWidth(), getHeight()), code, day, cloud, rain);
    }
}
