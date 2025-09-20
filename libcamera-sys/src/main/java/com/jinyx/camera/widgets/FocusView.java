package com.jinyx.camera.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FocusView extends View {

    private final float FOCUS_RADIUS;
    private final float SUN_RADIUS;
    private final float SUM_MOVE_RATIO;

    private static final long FOCUS_SHOW_TIME = 5_000L;

    private final Paint paint;
    private float lastMoveX, lastMoveY;
    private float focusX, focusY;
    private float sunX, sunY;

    private int focusAlpha = 0;
    private Thread alphaThread;
    private boolean hasMoveSun = false;

    private FocusViewListener focusViewListener;

    public FocusView(Context context) {
        this(context, null);
    }

    public FocusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(2);

        FOCUS_RADIUS = dip2px(30);
        SUN_RADIUS = dip2px(12);
        SUM_MOVE_RATIO = dip2px(16);
    }

    private float dip2px(float dip) {
        return getContext().getResources().getDisplayMetrics().density * dip;
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                hasMoveSun = false;
                interruptThread();
                lastMoveX = event.getX();
                lastMoveY = event.getY();
            }
            break;
            case MotionEvent.ACTION_MOVE: {
                // 判断是否是滑动事件，以及修改对应的曝光补偿
                if (focusAlpha > 0) {
                    if (event.getY() - lastMoveY > SUM_MOVE_RATIO) {    // 向下
                        sunY = Math.min(sunY + ((event.getY() - lastMoveY) / SUM_MOVE_RATIO), focusY + FOCUS_RADIUS * 1.6F);
                        lastMoveY = event.getY();
                        startMoveSun();
                    } else if (lastMoveY - event.getY() > SUM_MOVE_RATIO) {     // 向上
                        sunY = Math.max(sunY - ((lastMoveY - event.getY()) / SUM_MOVE_RATIO), focusY - FOCUS_RADIUS * 1.6F);
                        lastMoveY = event.getY();
                        startMoveSun();
                    }
                } else {
                    // 没有显示对焦框，但是用户是滑动事件，不显示对焦框
                    float deltaX = Math.abs(event.getX() - lastMoveX);
                    float deltaY = Math.abs(event.getY() - lastMoveY);
                    double sqrt = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                    int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                    if (sqrt > 2 * touchSlop) {
                        hasMoveSun = true;
                    }
                }
            }
            break;
            case MotionEvent.ACTION_UP: {
                if (!hasMoveSun) {
                    if (focusViewListener != null) {
                        focusViewListener.onFocusChange(event.getX(), event.getY(), getWidth(), getHeight());
                    }
                    focusX = event.getX();
                    sunY = focusY = event.getY();
                    startAlphaThread();
                }
            }
            break;
            default:
                break;
        }
        return true;
    }

    private void interruptThread() {
        if (alphaThread != null && alphaThread.isAlive() && !alphaThread.isInterrupted()) {
            alphaThread.interrupt();
            alphaThread = null;
        }
    }

    private void startAlphaThread() {
        interruptThread();
        focusAlpha = 255;
        alphaThread = new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                long deltaTime;
                do {
                    deltaTime = System.currentTimeMillis() - startTime;
                    focusAlpha = 255 - (int) (deltaTime * 255F / FOCUS_SHOW_TIME);
                    postInvalidate();
                    Thread.sleep(20);
                } while (deltaTime < FOCUS_SHOW_TIME);
                focusX = focusY = sunX = sunY = 0;
            } catch (InterruptedException ignore) {}
        });
        alphaThread.start();
    }

    private void startMoveSun() {
        focusAlpha = 255;
        hasMoveSun = true;
        if (focusViewListener != null) {
            focusViewListener.onExposureChange((focusY - sunY) / (FOCUS_RADIUS * 1.6F));
        }
        invalidate();
        startAlphaThread();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (focusAlpha <= 0) {
            return;
        }
        paint.setAlpha(focusAlpha);
        float focusLeft = focusX - FOCUS_RADIUS;
        float focusRight = focusX + FOCUS_RADIUS;

        // 画对焦框
        canvas.drawRect(focusLeft, focusY - FOCUS_RADIUS, focusRight, focusY + FOCUS_RADIUS, paint);
        canvas.drawLine(focusLeft, focusY, focusX - FOCUS_RADIUS * 5F / 6, focusY, paint);
        canvas.drawLine(focusRight, focusY, focusX + FOCUS_RADIUS * 5F / 6, focusY, paint);
        canvas.drawLine(focusX, focusY - FOCUS_RADIUS, focusX, focusY - FOCUS_RADIUS * 5F / 6, paint);
        canvas.drawLine(focusX, focusY + FOCUS_RADIUS, focusX, focusY + FOCUS_RADIUS * 5F / 6, paint);

        if (getWidth() - focusRight >= SUN_RADIUS * 4) {
            sunX = focusRight + SUN_RADIUS * 2;
        } else {
            sunX = focusLeft - SUN_RADIUS * 2;
        }
        float lineTop = focusY - FOCUS_RADIUS * 1.6F - SUN_RADIUS;
        float lineBottom = focusY + FOCUS_RADIUS * 1.6F + SUN_RADIUS;
        // 画太阳
        float unit = SUN_RADIUS / 5;
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(sunX, sunY, unit * 2, paint);
        paint.setStyle(Paint.Style.STROKE);

        canvas.drawLine(sunX - SUN_RADIUS, sunY, sunX - SUN_RADIUS + unit * 2, sunY, paint);
        canvas.drawLine(sunX + SUN_RADIUS, sunY, sunX + SUN_RADIUS - unit * 2, sunY, paint);
        canvas.drawLine(sunX, sunY - SUN_RADIUS, sunX, sunY - SUN_RADIUS + unit * 2, paint);
        canvas.drawLine(sunX, sunY + SUN_RADIUS, sunX, sunY + SUN_RADIUS - unit * 2, paint);

        float sin = (float) Math.sin(45 * Math.PI / 180);
        float bigEdgeLen = SUN_RADIUS * sin;
        float smallEdgeLen = unit * 2 * sin;
        canvas.drawLine(sunX + bigEdgeLen, sunY + bigEdgeLen, sunX + bigEdgeLen - smallEdgeLen, sunY + bigEdgeLen - smallEdgeLen, paint);
        canvas.drawLine(sunX + bigEdgeLen, sunY - bigEdgeLen, sunX + bigEdgeLen - smallEdgeLen, sunY - bigEdgeLen + smallEdgeLen, paint);
        canvas.drawLine(sunX - bigEdgeLen, sunY + bigEdgeLen, sunX - bigEdgeLen + smallEdgeLen, sunY + bigEdgeLen - smallEdgeLen, paint);
        canvas.drawLine(sunX - bigEdgeLen, sunY - bigEdgeLen, sunX - bigEdgeLen + smallEdgeLen, sunY - bigEdgeLen + smallEdgeLen, paint);

        // 画曝光补偿线
        if (sunY - SUN_RADIUS * 6F / 5 > lineTop) {
            canvas.drawLine(sunX, lineTop, sunX, sunY - SUN_RADIUS * 6F / 5, paint);
        }
        if (sunY + SUN_RADIUS * 6F / 5 < lineBottom) {
            canvas.drawLine(sunX, sunY + SUN_RADIUS * 6F / 5, sunX, lineBottom, paint);
        }
    }

    /**
     * 隐藏对焦框，例如切换摄像头、拍照时
     */
    public void hideFocus() {
        if (focusAlpha > 0) {
            interruptThread();
            focusX = focusY = sunX = sunY = 0;
            focusAlpha = 0;
            invalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        interruptThread();
    }

    public void setOnFocusViewListener(FocusViewListener listener) {
        this.focusViewListener = listener;
    }

    public interface FocusViewListener {
        void onFocusChange(float x, float y, int width, int height);
        void onExposureChange(float exposure);
    }

}
