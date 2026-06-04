package in.bikdocs.ludo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LudoBoardView extends View {

    private LudoGameEngine engine;
    private AudioEngine audioEngine;
    private Paint gridPaint;
    private Paint fillPaint;
    private Paint starPaint;
    private Paint tokenPaint;
    private Paint glowPaint;

    private float cellSize;
    private float boardOffset;
    private float boardSize;

    // Highlights & Selection
    private List<Integer> validTokens = new ArrayList<>();
    private OnTokenClickListener onTokenClickListener;

    // Movement Animation states
    private int animatingPlayerIdx = -1;
    private int animatingTokenIdx = -1;
    private int animationStepCurrent = 0;
    private int animationStepTarget = 0;
    private float animatedX = 0f;
    private float animatedY = 0f;
    private boolean isAnimating = false;

    // Pulse effect for valid moves
    private float pulseValue = 0f;
    private ValueAnimator pulseAnimator;

    public interface OnTokenClickListener {
        void onTokenClick(int playerIdx, int tokenIdx);
    }

    public LudoBoardView(Context context) {
        super(context);
        init();
    }

    public LudoBoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LudoBoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2f);
        gridPaint.setColor(Color.parseColor("#33FFFFFF")); // subtle white grid lines

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setStyle(Paint.Style.FILL);
        starPaint.setColor(Color.parseColor("#FFD700")); // gold stars

        tokenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tokenPaint.setStyle(Paint.Style.FILL);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(6f);

        // Continuous pulse animation for selectable tokens
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1200);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.addUpdateListener(animation -> {
            pulseValue = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    public void setEngine(LudoGameEngine engine) {
        this.engine = engine;
        invalidate();
    }

    public void setAudioEngine(AudioEngine audioEngine) {
        this.audioEngine = audioEngine;
    }

    public void setValidMoves(List<Integer> validTokens) {
        this.validTokens = validTokens;
        invalidate();
    }

    public void setOnTokenClickListener(OnTokenClickListener listener) {
        this.onTokenClickListener = listener;
    }

    // Animates a token step by step along its path
    public void animateTokenMovement(int playerIdx, int tokenIdx, int startPos, int targetPos, Runnable onFinish) {
        if (startPos == targetPos) {
            if (onFinish != null) onFinish.run();
            return;
        }

        animatingPlayerIdx = playerIdx;
        animatingTokenIdx = tokenIdx;
        animationStepCurrent = startPos;
        animationStepTarget = targetPos;
        isAnimating = true;

        animateNextStep(onFinish);
    }

    private void animateNextStep(Runnable onFinish) {
        if (animationStepCurrent >= animationStepTarget) {
            isAnimating = false;
            animatingPlayerIdx = -1;
            animatingTokenIdx = -1;
            invalidate();
            if (onFinish != null) onFinish.run();
            return;
        }

        // Fetch coordinate points
        int nextStep = animationStepCurrent + 1;
        float[] startPx = getTokenPixelCenter(animatingPlayerIdx, animatingTokenIdx, animationStepCurrent);
        float[] endPx = getTokenPixelCenter(animatingPlayerIdx, animatingTokenIdx, nextStep);

        float startX = startPx[0];
        float startY = startPx[1];
        float endX = endPx[0];
        float endY = endPx[1];

        ValueAnimator stepAnimator = ValueAnimator.ofFloat(0f, 1f);
        stepAnimator.setDuration(250);
        stepAnimator.setInterpolator(new DecelerateInterpolator());
        stepAnimator.addUpdateListener(animation -> {
            float frac = (float) animation.getAnimatedValue();
            // Jumpy arch trajectory (parabola for bounce effect)
            float jumpHeight = cellSize * 0.5f;
            animatedX = startX + (endX - startX) * frac;
            animatedY = startY + (endY - startY) * frac - (float) Math.sin(frac * Math.PI) * jumpHeight;
            invalidate();
        });

        stepAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                // Play step audio when the jump starts
                if (audioEngine != null) {
                    audioEngine.playStepSound();
                }
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                animationStepCurrent++;
                animateNextStep(onFinish);
            }
        });

        stepAnimator.start();
    }

    private LudoGameEngine.Point getPointForPosition(int playerIdx, int tokenIdx, int position) {
        return engine.getCoordinateForPosition(playerIdx, tokenIdx, position);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (engine == null) return;

        int width = getWidth();
        boardSize = width;
        cellSize = boardSize / 15f;

        // Draw Board Background
        fillPaint.setColor(Color.parseColor("#1B1B1B"));
        canvas.drawRect(0, 0, boardSize, boardSize, fillPaint);

        // Draw the 4 Home Yards
        // P0 (Red) is now BL
        drawHomeYard(canvas, 0, 9, 6, 15, Color.parseColor("#FF4B4B"), Color.parseColor("#80FF4B4B")); // Red
        // P1 (Green) is now TL
        drawHomeYard(canvas, 0, 0, 6, 6, Color.parseColor("#2ECC71"), Color.parseColor("#802ECC71")); // Green
        // P2 (Yellow) is now TR
        drawHomeYard(canvas, 9, 0, 15, 6, Color.parseColor("#F1C40F"), Color.parseColor("#80F1C40F")); // Yellow
        // P3 (Blue) is now BR
        drawHomeYard(canvas, 9, 9, 15, 15, Color.parseColor("#3498DB"), Color.parseColor("#803498DB")); // Blue

        // Draw standard cells
        drawCells(canvas);

        // Draw central Home Triangle
        drawHomeTriangle(canvas);

        // Draw Stars / Safe cells
        drawStars(canvas);

        // Draw active Tokens
        drawTokens(canvas);
    }

    private void drawHomeYard(Canvas canvas, int x1, int y1, int x2, int y2, int primaryColor, int lightColor) {
        float left = x1 * cellSize;
        float top = y1 * cellSize;
        float right = x2 * cellSize;
        float bottom = y2 * cellSize;

        // Outer yard box
        fillPaint.setShader(new LinearGradient(left, top, right, bottom, primaryColor, darkenColor(primaryColor), Shader.TileMode.CLAMP));
        RectF outerRect = new RectF(left + 4, top + 4, right - 4, bottom - 4);
        canvas.drawRoundRect(outerRect, 24, 24, fillPaint);
        fillPaint.setShader(null);
        
        // Stroke for outer box
        gridPaint.setColor(Color.parseColor("#111111"));
        canvas.drawRoundRect(outerRect, 24, 24, gridPaint);

        // Inner dark box
        fillPaint.setColor(Color.parseColor("#121212"));
        RectF innerRect = new RectF(left + cellSize * 1.2f, top + cellSize * 1.2f, right - cellSize * 1.2f, bottom - cellSize * 1.2f);
        canvas.drawRoundRect(innerRect, 16, 16, fillPaint);
        canvas.drawRoundRect(innerRect, 16, 16, gridPaint);

        // 4 Token spots
        float offset = cellSize * 1.8f;
        float r = cellSize * 0.45f;
        
        float[][] spots = {
            {left + offset, top + offset},
            {right - offset, top + offset},
            {left + offset, bottom - offset},
            {right - offset, bottom - offset}
        };

        fillPaint.setColor(primaryColor);
        for (float[] spot : spots) {
            canvas.drawCircle(spot[0], spot[1], r, fillPaint);
            canvas.drawCircle(spot[0], spot[1], r, gridPaint);
        }
        
        // Restore gridPaint for the rest of the board
        gridPaint.setColor(Color.parseColor("#33FFFFFF"));
    }

    private void drawCells(Canvas canvas) {
        // Red track cell paths (1..51)
        for (int x = 0; x < 15; x++) {
            for (int y = 0; y < 15; y++) {
                // Skip home yards and home triangles
                if ((x < 6 && y < 6) || (x >= 9 && y < 6) || (x >= 9 && y >= 9) || (x < 6 && y >= 9)) {
                    continue;
                }
                if (x >= 6 && x <= 8 && y >= 6 && y <= 8) {
                    continue; // home triangle
                }

                float left = x * cellSize;
                float top = y * cellSize;
                float right = left + cellSize;
                float bottom = top + cellSize;

                // Color home path lanes
                if (x == 7 && y >= 9 && y <= 13) {
                    fillPaint.setColor(Color.parseColor("#FF4B4B")); // Red home run (BL)
                    canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, fillPaint);
                } else if (y == 7 && x >= 1 && x <= 5) {
                    fillPaint.setColor(Color.parseColor("#2ECC71")); // Green home run (TL)
                    canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, fillPaint);
                } else if (x == 7 && y >= 1 && y <= 5) {
                    fillPaint.setColor(Color.parseColor("#F1C40F")); // Yellow home run (TR)
                    canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, fillPaint);
                } else if (y == 7 && x >= 9 && x <= 13) {
                    fillPaint.setColor(Color.parseColor("#3498DB")); // Blue home run (BR)
                    canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, fillPaint);
                } 
                // Color entry points
                else if (x == 6 && y == 13) {
                    fillPaint.setColor(Color.parseColor("#FF4B4B")); // Red start (BL)
                    canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, fillPaint);
                } else if (x == 1 && y == 6) {
                    fillPaint.setColor(Color.parseColor("#2ECC71")); // Green start (TL)
                    canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, fillPaint);
                } else if (x == 8 && y == 1) {
                    fillPaint.setColor(Color.parseColor("#F1C40F")); // Yellow start (TR)
                    canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, fillPaint);
                } else if (x == 13 && y == 8) {
                    fillPaint.setColor(Color.parseColor("#3498DB")); // Blue start (BR)
                    canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, fillPaint);
                } else {
                    // Regular grid tile
                    fillPaint.setColor(Color.parseColor("#222222"));
                    canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, fillPaint);
                }

                // Grid outlines
                canvas.drawRect(left, top, right, bottom, gridPaint);
            }
        }
    }

    private void drawHomeTriangle(Canvas canvas) {
        float cx = boardSize / 2f;
        float cy = boardSize / 2f;
        float size = cellSize * 1.5f;

        // Left Triangle (Green - TL)
        fillPaint.setColor(Color.parseColor("#2ECC71"));
        Path leftPath = new Path();
        leftPath.moveTo(6 * cellSize, 6 * cellSize);
        leftPath.lineTo(cx, cy);
        leftPath.lineTo(6 * cellSize, 9 * cellSize);
        leftPath.close();
        canvas.drawPath(leftPath, fillPaint);

        // Top Triangle (Yellow - TR)
        fillPaint.setColor(Color.parseColor("#F1C40F"));
        Path topPath = new Path();
        topPath.moveTo(6 * cellSize, 6 * cellSize);
        topPath.lineTo(cx, cy);
        topPath.lineTo(9 * cellSize, 6 * cellSize);
        topPath.close();
        canvas.drawPath(topPath, fillPaint);

        // Right Triangle (Blue - BR)
        fillPaint.setColor(Color.parseColor("#3498DB"));
        Path rightPath = new Path();
        rightPath.moveTo(9 * cellSize, 6 * cellSize);
        rightPath.lineTo(cx, cy);
        rightPath.lineTo(9 * cellSize, 9 * cellSize);
        rightPath.close();
        canvas.drawPath(rightPath, fillPaint);

        // Bottom Triangle (Red - BL)
        fillPaint.setColor(Color.parseColor("#FF4B4B"));
        Path bottomPath = new Path();
        bottomPath.moveTo(6 * cellSize, 9 * cellSize);
        bottomPath.lineTo(cx, cy);
        bottomPath.lineTo(9 * cellSize, 9 * cellSize);
        bottomPath.close();
        canvas.drawPath(bottomPath, fillPaint);

        // Outline borders
        canvas.drawLine(6 * cellSize, 6 * cellSize, 9 * cellSize, 9 * cellSize, gridPaint);
        canvas.drawLine(6 * cellSize, 9 * cellSize, 9 * cellSize, 6 * cellSize, gridPaint);
        canvas.drawRect(6 * cellSize, 6 * cellSize, 9 * cellSize, 9 * cellSize, gridPaint);
    }

    private void drawStars(Canvas canvas) {
        int[][] stars = {
            {1, 6}, {6, 2}, {8, 1}, {12, 6},
            {13, 8}, {8, 12}, {6, 13}, {2, 8}
        };

        for (int[] s : stars) {
            drawStarSymbol(canvas, s[0], s[1]);
        }
    }

    private void drawStarSymbol(Canvas canvas, int cx, int cy) {
        float x = getCellCenterX(cx);
        float y = getCellCenterY(cy);
        float r = cellSize * 0.35f;

        Path path = new Path();
        double angle = Math.PI / 5;
        path.moveTo(x, y - r);
        for (int i = 1; i < 10; i++) {
            float currR = (i % 2 == 0) ? r : r * 0.4f;
            float px = x + (float) Math.sin(i * angle) * currR;
            float py = y - (float) Math.cos(i * angle) * currR;
            path.lineTo(px, py);
        }
        path.close();
        canvas.drawPath(path, starPaint);
    }

    private void drawTokens(Canvas canvas) {
        int[][] positions = engine.getTokenPositions();
        int curPlayerIdx = engine.getCurrentPlayerIndex();

        // Pass 1: Draw tokens for all players EXCEPT the current player
        for (int p = 0; p < 4; p++) {
            if (!engine.isPlayerActive(p) || p == curPlayerIdx) continue;
            drawPlayerTokens(canvas, p, positions, curPlayerIdx);
        }

        // Pass 2: Draw tokens for the current player LAST (so they are always on top)
        if (engine.isPlayerActive(curPlayerIdx)) {
            drawPlayerTokens(canvas, curPlayerIdx, positions, curPlayerIdx);
        }
    }

    private void drawPlayerTokens(Canvas canvas, int p, int[][] positions, int curPlayerIdx) {
        int color = getPlayerColorValue(p);

        for (int t = 0; t < 4; t++) {
            // If animating this specific token, handle rendering differently
            if (isAnimating && animatingPlayerIdx == p && animatingTokenIdx == t) {
                drawSingleToken(canvas, animatedX, animatedY, color, false, p, t);
                continue;
            }

            // Check for overlapping tokens to draw offsets
            int pos = positions[p][t];
            float[] pxCenter = getTokenPixelCenter(p, t, pos);
            float cx = pxCenter[0];
            float cy = pxCenter[1];

            LudoGameEngine.Point coord = getPointForPosition(p, t, pos);

            // Offset overlaps
            List<Integer> overlaps = getOverlappingTokens(coord, p, t);
            if (!overlaps.isEmpty()) {
                int total = overlaps.size() + 1;
                // Current token gets index 0 in the offset ring
                float angle = (float) (2 * Math.PI * 0 / total);
                float offsetR = cellSize * 0.25f;
                cx += Math.cos(angle) * offsetR;
                cy += Math.sin(angle) * offsetR;
            }

            // Is selectable
            boolean selectable = (p == curPlayerIdx) && validTokens.contains(t) && !isAnimating;

            drawSingleToken(canvas, cx, cy, color, selectable, p, t);
        }
    }

    private void drawSingleToken(Canvas canvas, float cx, float cy, int color, boolean selectable, int playerIdx, int tokenIdx) {
        float r = cellSize * 0.45f;

        // Glow ring if selectable
        if (selectable) {
            glowPaint.setColor(Color.parseColor("#FFD700"));
            glowPaint.setAlpha((int) (100 + pulseValue * 155));
            canvas.drawCircle(cx, cy, r + 4f + pulseValue * 8f, glowPaint);
        }

        // Shadow
        fillPaint.setColor(Color.parseColor("#99000000"));
        canvas.drawCircle(cx + 2, cy + 4, r, fillPaint);

        // Core Gradient
        fillPaint.setShader(new RadialGradient(cx - r * 0.2f, cy - r * 0.2f, r * 1.2f, Color.WHITE, color, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, fillPaint);
        fillPaint.setShader(null);

        // Border outline
        glowPaint.setColor(Color.parseColor("#DDFFFFFF"));
        glowPaint.setStrokeWidth(2.5f);
        canvas.drawCircle(cx, cy, r, glowPaint);

        // Ring inner design
        fillPaint.setColor(darkenColor(color));
        canvas.drawCircle(cx, cy, r * 0.4f, fillPaint);

        // Center white dot
        fillPaint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, r * 0.15f, fillPaint);
    }

    private List<Integer> getOverlappingTokens(LudoGameEngine.Point target, int pIdx, int tIdx) {
        List<Integer> list = new ArrayList<>();
        int[][] positions = engine.getTokenPositions();
        for (int p = 0; p < 4; p++) {
            if (!engine.isPlayerActive(p)) continue;
            for (int t = 0; t < 4; t++) {
                if (p == pIdx && t == tIdx) continue;
                if (positions[p][t] == LudoGameEngine.POSITION_YARD) continue;
                LudoGameEngine.Point c = getPointForPosition(p, t, positions[p][t]);
                if (c.x == target.x && c.y == target.y) {
                    list.add(t);
                }
            }
        }
        return list;
    }

    private float getCellCenterX(int x) {
        return x * cellSize + cellSize / 2f;
    }

    private float getCellCenterY(int y) {
        return y * cellSize + cellSize / 2f;
    }

    private float[] getTokenPixelCenter(int p, int t, int pos) {
        if (pos == LudoGameEngine.POSITION_YARD) {
            float left = 0, top = 0;
            if (p == 0) { left = 0; top = 9 * cellSize; } // Red (BL)
            else if (p == 1) { left = 0; top = 0; } // Green (TL)
            else if (p == 2) { left = 9 * cellSize; top = 0; } // Yellow (TR)
            else if (p == 3) { left = 9 * cellSize; top = 9 * cellSize; } // Blue (BR)

            float right = left + 6 * cellSize;
            float bottom = top + 6 * cellSize;
            float offset = cellSize * 1.8f;

            if (t == 0) return new float[]{left + offset, top + offset};
            if (t == 1) return new float[]{right - offset, top + offset};
            if (t == 2) return new float[]{left + offset, bottom - offset};
            if (t == 3) return new float[]{right - offset, bottom - offset};
        }

        LudoGameEngine.Point coord = getPointForPosition(p, t, pos);
        return new float[]{getCellCenterX(coord.x), getCellCenterY(coord.y)};
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isAnimating || engine == null || onTokenClickListener == null) return false;

            float tx = event.getX();
            float ty = event.getY();

            // Find clicked token
            int curPlayerIdx = engine.getCurrentPlayerIndex();
            int[][] positions = engine.getTokenPositions();

            for (int t : validTokens) {
                int pos = positions[curPlayerIdx][t];
                float[] pxCenter = getTokenPixelCenter(curPlayerIdx, t, pos);
                float cx = pxCenter[0];
                float cy = pxCenter[1];

                float dx = tx - cx;
                float dy = ty - cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist < cellSize * 0.6f) {
                    onTokenClickListener.onTokenClick(curPlayerIdx, t);
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    // Color helpers
    private int getPlayerColorValue(int playerIdx) {
        switch (playerIdx) {
            case 0: return Color.parseColor("#FF4B4B");
            case 1: return Color.parseColor("#2ECC71");
            case 2: return Color.parseColor("#F1C40F");
            case 3: return Color.parseColor("#3498DB");
            default: return Color.GRAY;
        }
    }

    private int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.65f; // reduce brightness
        return Color.HSVToColor(hsv);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
    }
}
