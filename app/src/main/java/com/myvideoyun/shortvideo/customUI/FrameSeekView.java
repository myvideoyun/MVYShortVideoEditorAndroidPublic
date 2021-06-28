package com.myvideoyun.shortvideo.customUI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.myvideoyun.shortvideo.R;

/**
 * Created by yangshunfa on 2019/3/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class FrameSeekView extends View {

    private int measuredHeight;
    private int measuredWidth;
    private Point startPoint;
    private Point endPoint;
    private Rect endSliderRect;
    private Rect startSliderRect;
    private Paint mPaint;
    private Rect touchSlider;
    private float mMaxLength = 1000;// 进度条最大长度
    private float mMinLength = 500;// 进度条最小选中
    private float mStartPosition = 0;// 默认开始
    private float mEndPosition = 0;// 默认结束
    private int sliderWidth = 30;// slider 半宽

    private OnChooseListener listener;

    public FrameSeekView(Context context) {
        super(context);
        initView(context);
    }

    public FrameSeekView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public FrameSeekView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(getResources().getColor(R.color.mxyTheme));
        setFocusableInTouchMode(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        measuredHeight = getMeasuredHeight();
        measuredWidth = getMeasuredWidth();
        // 计算默认的头和尾的两个 slider 的点
        startPoint = new Point(sliderWidth + getPaddingStart(), measuredHeight / 2 + getPaddingTop());
        endPoint = new Point(measuredWidth - sliderWidth - getPaddingEnd() , measuredHeight / 2 - getPaddingBottom());

        startSliderRect = new Rect(startPoint.x - sliderWidth,0, startPoint.x + sliderWidth, measuredHeight - getPaddingBottom());
        endSliderRect = new Rect(endPoint.x - sliderWidth,0, endPoint.x + sliderWidth, measuredHeight - getPaddingBottom());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(startSliderRect, mPaint);
        canvas.drawRect(endSliderRect, mPaint);
        canvas.drawLine(startSliderRect.right, startSliderRect.top, endSliderRect.left, endSliderRect.top, mPaint);
        canvas.drawLine(startSliderRect.right, startSliderRect.bottom, endSliderRect.left, endSliderRect.bottom, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                touchSlider = getTouchSlider(event);
                if (touchSlider != null){
                    if (listener != null){
                        listener.onTouchedSlider();
                    }
                    return true;
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                if (touchSlider != null){
                    int right = (int) (x + sliderWidth);
                    int left = (int) (x - sliderWidth);

                    if (touchSlider == startSliderRect ){
                        if (left<startPoint.x - sliderWidth) break;// 超过最左边
                        if (checkMin() && (left > startSliderRect.left)){
//                            int interval = (int) (mMaxLength/ measuredWidth * mMinLength);
//                            startSliderRect.left = endSliderRect.left - interval;
//                            startSliderRect.right = endSliderRect.right - interval;
                            return false;// 不能小于最小值
                        }
                        if (listener != null){
                            listener.onSlideStart(touchSlider.left * mMaxLength / measuredWidth);
                        }
                    }
                    if (touchSlider == endSliderRect){
                        if (right > endPoint.x + sliderWidth) break;// 超过最右边
                        if (checkMin() && (right < endSliderRect.right)){
//                            int interval = (int) (mMaxLength/ measuredWidth * mMinLength);
//                            endSliderRect.left = startSliderRect.left + interval;
//                            endSliderRect.right = startSliderRect.right + interval;
                            return false;// 不能小于最小值
                        }

                        if (listener != null){
                            listener.onSlideEnd(touchSlider.right * mMaxLength / measuredWidth);
                        }
                    }
                    touchSlider.right = right;
                    touchSlider.left = left;

                    invalidate();
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
                if (touchSlider != null) {
                    if (listener != null) {
                        listener.onChoose(startSliderRect.left * mMaxLength / measuredWidth, endSliderRect.right * mMaxLength / measuredWidth);
                    }
                    touchSlider = null;
                    return true;
                }
                return false;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 检测两个滑块之间值是否小于最小值
     * @return
     */
    private boolean checkMin() {
        float interval = (endSliderRect.right - startSliderRect.left) * mMaxLength / measuredWidth;
        return interval <= mMinLength;
    }

    private Rect getTouchSlider(MotionEvent event) {
        float x = event.getX();
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        float y = event.getY();
        if (x >= startSliderRect.left && x <= startSliderRect.right && y >= startSliderRect.top && y <= startSliderRect.bottom){
            return startSliderRect;
        }
        if (x >= endSliderRect.left && x <= endSliderRect.right && y >= endSliderRect.top && y <= endSliderRect.bottom){
            return endSliderRect;
        }
        return null;
    }

    public float getMaxLength() {
        return mMaxLength;
    }

    public void setMaxLength(float maxLength) {
        this.mMaxLength = maxLength;
    }

    public float getStartPosition() {
        return mStartPosition;
    }

    public void setStartPosition(float startPosition) {
        this.mStartPosition = startPosition;
    }

    public float getEndPosition() {
        return mEndPosition;
    }

    public void setEndPosition(float endPosition) {
        this.mEndPosition = endPosition;
    }

    public float getMinLength() {
        return mMinLength;
    }

    public void setMinLength(float minLength) {
        this.mMinLength = minLength;
    }

    public void setListener(OnChooseListener l){
        this.listener = l;
    }

    public interface OnChooseListener{
        void onSlideStart(float position);
        void onSlideEnd(float position);
        void onChoose(float start, float end);
        void onTouchedSlider();
    }
}
