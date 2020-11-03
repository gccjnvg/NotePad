// ColorPickerView.java
package com.example.android.notepad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {

    private Paint mPaint;
    private Paint mCenterPaint;
    private int[] mColors;
    private boolean mTrackingCenter;
    private boolean mHighlightCenter;
    private OnColorChangedListener mListener;

    public interface OnColorChangedListener {
        void colorChanged(int color);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mColors = new int[] {
                0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00,
                0xFFFFFF00, 0xFFFF0000
        };

        Shader s = new SweepGradient(0, 0, mColors, null);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setShader(s);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(32);

        mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCenterPaint.setColor(Color.BLACK);
        mCenterPaint.setStrokeWidth(5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = Math.min(getWidth(), getHeight()) / 2 - mPaint.getStrokeWidth() * 0.5f;

        canvas.translate(getWidth() / 2, getHeight() / 2);

        canvas.drawCircle(0, 0, r, mPaint);
        canvas.drawCircle(0, 0, r * 0.5f, mCenterPaint);

        if (mTrackingCenter) {
            int c = mCenterPaint.getColor();
            mCenterPaint.setStyle(Paint.Style.STROKE);

            if (mHighlightCenter) {
                mCenterPaint.setAlpha(0xFF);
            } else {
                mCenterPaint.setAlpha(0x80);
            }
            canvas.drawCircle(0, 0, r * 0.5f + mCenterPaint.getStrokeWidth(), mCenterPaint);

            mCenterPaint.setStyle(Paint.Style.FILL);
            mCenterPaint.setColor(c);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() - getWidth() / 2;
        float y = event.getY() - getHeight() / 2;
        boolean inCenter = Math.sqrt(x * x + y * y) <= (Math.min(getWidth(), getHeight()) / 2 - mPaint.getStrokeWidth() * 0.5f) * 0.5f;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTrackingCenter = inCenter;
                if (inCenter) {
                    mHighlightCenter = true;
                    invalidate();
                    break;
                }
            case MotionEvent.ACTION_MOVE:
                if (mTrackingCenter) {
                    if (mHighlightCenter != inCenter) {
                        mHighlightCenter = inCenter;
                        invalidate();
                    }
                } else {
                    float angle = (float) Math.atan2(y, x);
                    // Convert to degrees
                    float unit = (float) (angle / (2 * Math.PI));
                    if (unit < 0) {
                        unit += 1;
                    }
                    // Update color based on angle
                    // This is a simplified implementation
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mTrackingCenter) {
                    if (inCenter) {
                        if (mListener != null) {
                            mListener.colorChanged(mCenterPaint.getColor());
                        }
                    }
                    mTrackingCenter = false;
                    invalidate();
                }
                break;
        }
        return true;
    }

    public void setColor(int color) {
        mCenterPaint.setColor(color);
        invalidate();
    }

    public int getColor() {
        return mCenterPaint.getColor();
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }
}
