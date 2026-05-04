package com.example.photos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorWheelView extends View {

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    private Bitmap wheelBitmap;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float hue = 270f;        // 0-360
    private float saturation = 0.8f; // 0-1
    private float brightness = 1f;   // 0-1  (controlled by brightness bar)

    private float wheelRadius;
    private float cx, cy;

    // Brightness bar
    private final RectF barRect = new RectF();
    private static final float BAR_H = 28f;
    private static final float BAR_MARGIN = 20f;

    private OnColorChangedListener listener;

    public ColorWheelView(Context ctx) { super(ctx); init(); }
    public ColorWheelView(Context ctx, AttributeSet a) { super(ctx, a); init(); }

    private void init() {
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(3f);
        // Dark ring so the selector dot is visible against both light and dark dialog backgrounds
        selectorPaint.setColor(0xFF222222);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setColor(0x44000000);
    }

    public void setOnColorChangedListener(OnColorChangedListener l) { this.listener = l; }

    public int getColor() {
        float[] hsv = {hue, saturation, brightness};
        return Color.HSVToColor(hsv);
    }

    public void setColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hue = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        float barH = dpToPx(BAR_H);
        float margin = dpToPx(BAR_MARGIN);
        float availH = h - barH - margin;
        wheelRadius = Math.min(w, availH) / 2f - dpToPx(8);
        cx = w / 2f;
        cy = wheelRadius + dpToPx(8);
        barRect.set(dpToPx(16), cy + wheelRadius + margin,
                    w - dpToPx(16), cy + wheelRadius + margin + barH);
        buildWheelBitmap();
    }

    private void buildWheelBitmap() {
        if (wheelRadius <= 0) return;
        int size = (int)(wheelRadius * 2 + 2);
        wheelBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        float r = wheelRadius;
        float bcx = r, bcy = r;
        float[] hsv = new float[3];
        hsv[2] = brightness;
        int[] pixels = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - bcx, dy = y - bcy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= r) {
                    hsv[0] = (float)((Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360);
                    hsv[1] = dist / r;
                    pixels[y * size + x] = Color.HSVToColor(hsv);
                }
            }
        }
        wheelBitmap.setPixels(pixels, 0, size, 0, 0, size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (wheelBitmap == null) return;

        // Draw wheel
        canvas.drawBitmap(wheelBitmap, cx - wheelRadius, cy - wheelRadius, paint);

        // Draw selector dot on wheel
        double angle = Math.toRadians(hue);
        float sx = cx + (float)(saturation * wheelRadius * Math.cos(angle));
        float sy = cy + (float)(saturation * wheelRadius * Math.sin(angle));
        paint.setColor(getColor());
        canvas.drawCircle(sx, sy, dpToPx(10), paint);
        canvas.drawCircle(sx, sy, dpToPx(10), selectorPaint);

        // Draw brightness bar
        drawBrightnessBar(canvas);

        // Draw brightness thumb
        float bx = barRect.left + brightness * barRect.width();
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(barRect, dpToPx(14), dpToPx(14), borderPaint);
        paint.setColor(getColor());
        canvas.drawCircle(bx, barRect.centerY(), dpToPx(14), paint);
        canvas.drawCircle(bx, barRect.centerY(), dpToPx(14), selectorPaint);
    }

    private void drawBrightnessBar(Canvas canvas) {
        // Gradient from black to the full-hue color
        float[] hsvFull = {hue, saturation, 1f};
        int fullColor = Color.HSVToColor(hsvFull);
        android.graphics.LinearGradient lg = new android.graphics.LinearGradient(
                barRect.left, 0, barRect.right, 0,
                Color.BLACK, fullColor, android.graphics.Shader.TileMode.CLAMP);
        paint.setShader(lg);
        canvas.drawRoundRect(barRect, dpToPx(14), dpToPx(14), paint);
        paint.setShader(null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        if (e.getAction() == MotionEvent.ACTION_DOWN
                || e.getAction() == MotionEvent.ACTION_MOVE) {

            // Check brightness bar
            if (y >= barRect.top - dpToPx(10) && y <= barRect.bottom + dpToPx(10)) {
                brightness = Math.max(0f, Math.min(1f,
                        (x - barRect.left) / barRect.width()));
                buildWheelBitmap();
                invalidate();
                notifyListener();
                return true;
            }

            // Check wheel
            float dx = x - cx, dy = y - cy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist <= wheelRadius + dpToPx(8)) {
                hue = (float)((Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360);
                saturation = Math.min(1f, dist / wheelRadius);
                invalidate();
                notifyListener();
                return true;
            }
        }
        return super.onTouchEvent(e);
    }

    private void notifyListener() {
        if (listener != null) listener.onColorChanged(getColor());
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
