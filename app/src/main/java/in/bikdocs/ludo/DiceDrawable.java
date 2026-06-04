package in.bikdocs.ludo;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Custom Drawable that renders a dice face (1-6) with proper dot patterns.
 * Draws a rounded white square with black dots positioned correctly.
 */
public class DiceDrawable extends Drawable {

    private final int value;
    private final Paint bgPaint;
    private final Paint dotPaint;

    public DiceDrawable(int value) {
        this.value = Math.max(1, Math.min(6, value));

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.WHITE);
        bgPaint.setStyle(Paint.Style.FILL);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.parseColor("#1A1A1A"));
        dotPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int w = getBounds().width();
        int h = getBounds().height();
        int size = Math.min(w, h);
        float left = (w - size) / 2f;
        float top = (h - size) / 2f;

        // Draw rounded dice background
        RectF rect = new RectF(left + 2, top + 2, left + size - 2, top + size - 2);
        canvas.drawRoundRect(rect, size * 0.15f, size * 0.15f, bgPaint);

        // Dot positions (3x3 grid: TL, TC, TR, ML, MC, MR, BL, BC, BR)
        float pad = size * 0.25f;
        float cx = left + size / 2f;
        float cy = top + size / 2f;
        float lx = left + pad;
        float rx = left + size - pad;
        float ty = top + pad;
        float by = top + size - pad;
        float dotR = size * 0.08f;

        switch (value) {
            case 1:
                drawDot(canvas, cx, cy, dotR);
                break;
            case 2:
                drawDot(canvas, lx, ty, dotR);
                drawDot(canvas, rx, by, dotR);
                break;
            case 3:
                drawDot(canvas, lx, ty, dotR);
                drawDot(canvas, cx, cy, dotR);
                drawDot(canvas, rx, by, dotR);
                break;
            case 4:
                drawDot(canvas, lx, ty, dotR);
                drawDot(canvas, rx, ty, dotR);
                drawDot(canvas, lx, by, dotR);
                drawDot(canvas, rx, by, dotR);
                break;
            case 5:
                drawDot(canvas, lx, ty, dotR);
                drawDot(canvas, rx, ty, dotR);
                drawDot(canvas, cx, cy, dotR);
                drawDot(canvas, lx, by, dotR);
                drawDot(canvas, rx, by, dotR);
                break;
            case 6:
                drawDot(canvas, lx, ty, dotR);
                drawDot(canvas, rx, ty, dotR);
                drawDot(canvas, lx, cy, dotR);
                drawDot(canvas, rx, cy, dotR);
                drawDot(canvas, lx, by, dotR);
                drawDot(canvas, rx, by, dotR);
                break;
        }
    }

    private void drawDot(Canvas canvas, float x, float y, float r) {
        canvas.drawCircle(x, y, r, dotPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        bgPaint.setAlpha(alpha);
        dotPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        bgPaint.setColorFilter(colorFilter);
        dotPaint.setColorFilter(colorFilter);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public int getIntrinsicWidth() {
        return 96;
    }

    @Override
    public int getIntrinsicHeight() {
        return 96;
    }
}
