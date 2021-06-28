package com.myvideoyun.shortvideo.customUI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.page.effect.model.Effect;
import com.myvideoyun.shortvideo.page.effect.model.EffectModel;

import java.util.ArrayList;

/**
 * Created by yangshunfa on 2019/3/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class FrameRangeView extends View {

    private int measuredHeight;
    private int measuredWidth;
    //    private Point startPoint;
//    private Point endPoint;
//    private Rect endSliderRect;
    private Rect sliderRect;
    private Rect backgroudRect;
    private Paint mPaint;
    private Rect touchSlider;
    private float mMaxLength = 1000;// 进度条最大长度
    private float mMinLength = 10;// 进度条最小选中
    private float mStartPosition = 0;// 默认开始
    private float mEndPosition = 0;// 默认结束
//    private int sliderWidth = 20;// slider 半宽

    private OnChooseListener listener;
    private float downX;
    private int paddingLeft;
    private int paddingRight;
    private int right;
    private int left;
    private SliderRect currentRect;
    //    private Rect currentRect;

    public FrameRangeView(Context context) {
        super(context);
        initView(context);
    }

    public FrameRangeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public FrameRangeView(Context context, AttributeSet attrs, int defStyleAttr) {
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
//        startPoint = new Point(getPaddingStart(), measuredHeight / 2 + getPaddingTop());
//        endPoint = new Point(measuredWidth - sliderWidth - getPaddingEnd() , measuredHeight / 2 - getPaddingBottom());

        paddingLeft = getPaddingLeft();
        paddingRight = getPaddingRight();
        setRight();
//        endSliderRect = new Rect(endPoint.percent_x - sliderWidth,0, endPoint.percent_x + sliderWidth, measuredHeight);

    }

    private void setRight() {
        left = paddingLeft;
        right = (int) (measuredWidth * mMinLength / mMaxLength);
        sliderRect = new Rect(left, getPaddingTop(), right, measuredHeight - getPaddingBottom());
        backgroudRect = new Rect(left, measuredHeight / 4, measuredWidth - getPaddingEnd(), measuredHeight * 3 / 4);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(0x55000000);
        canvas.drawRect(backgroudRect, mPaint);

        // 绘制色块
        SliderRect colorRect = null;
        for (int i = 0; i < colorLumps.size(); i++) {
            colorRect = colorLumps.get(i);
            mPaint.setColor(colorRect.model.color);
            canvas.drawRect(colorRect.rect, mPaint);
        }

        mPaint.setColor(getResources().getColor(R.color.mxyTheme));
        canvas.drawRect(sliderRect, mPaint);
    }

//    private MotionEvent downTouchedEvnet = null;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean touchedRect = isTouchedRect(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                sliderRect.left = (int) downX - 10;
                sliderRect.right = (int) (downX - 10 + right);
                if (listener != null) {
                    float start = getStartProgress();
                    float end = getEndProgress();
                    listener.onSlideStart(start);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int newLeft = (int) event.getX() - 10;
                int newRight = (int) (event.getX() - 10 + this.right);
                if (newLeft > paddingLeft && newRight < measuredWidth - paddingRight) {
                    sliderRect.left = newLeft;
                    sliderRect.right = newRight;

                    if (listener != null) {
                        float start = getStartProgress();
                        float end = getEndProgress();
                        listener.onSliding(start, end);
                    }

                    Log.e("moose",  ", on touch event , right=" + sliderRect.right + ", left=" +sliderRect.left);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                downX = 0;

                if (listener != null) {
                    float start = getStartProgress();
                    float end = getEndProgress();
                    listener.onSlideEnd(start, end);
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private boolean isTouchedRect(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (x > sliderRect.left && x < sliderRect.right && y > sliderRect.top && y < sliderRect.bottom) {
            return true;
        }
        return false;
    }

    public float getMaxLength() {
        return mMaxLength;
    }

    public void setMaxLength(float maxLength) {
        this.mMaxLength = maxLength;
        setRight();
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
        setRight();
    }

    /**
     * 设置范围的开始
     * @param add
     */
    public void addProgress(float add, EffectModel model){
        float addValue = measuredWidth * add / mMaxLength;
        float right = sliderRect.right + addValue;
        float left = sliderRect.left + addValue;
        if (right >= measuredWidth - paddingRight){
            right = measuredWidth - paddingRight;
            left = right - (mMinLength / mMaxLength) * measuredWidth;
        }
        if (left < paddingLeft){
            left = paddingLeft;
            right = left + (mMinLength / mMaxLength) * measuredWidth;
        }
        sliderRect.left = (int) left;
        sliderRect.right = (int) right;
        // 只改变色块的right
        if (currentRect != null){
            if (right >= measuredWidth - paddingRight){
                left = right;
            }
            currentRect.rect.right = (int) left;
        }
        invalidate();
        if (listener != null) {
            float start = getStartProgress();
            float end = getEndProgress();
            listener.onSlideEnd(start, end);
        }
    }
    /**
     * 设置范围的开始
     * @param add
     */
    public void addProgressFromLast(float add, EffectModel model){
        float addValue = measuredWidth * add / mMaxLength;
        float right = sliderRect.right - addValue;
        float left = sliderRect.left - addValue;
        if (right >= measuredWidth - paddingRight){
            right = measuredWidth - paddingRight;
            left = right - (mMinLength / mMaxLength) * measuredWidth;
        }
        if (left < paddingLeft){
            left = paddingLeft;
            right = left + (mMinLength / mMaxLength) * measuredWidth;
        }
        sliderRect.left = (int) left;
        sliderRect.right = (int) right;
        // 只改变色块的 left
        if (currentRect != null){
            if (left <= paddingLeft){
                right = left;
            }
            currentRect.rect.left = (int) right;
        }
        invalidate();
        if (listener != null) {
            float start = getStartProgress();
            float end = getEndProgress();
            listener.onSlideEnd(start, end);
        }
//        Log.e("moose", "当前的色块是：right = " + currentRect.rect.left + " , right = " + currentRect.rect.right);
    }

    public ArrayList<SliderRect> colorLumps = new ArrayList<>();

    /**
     * length 是进度的长度，单位不是像素，是当前设置的最大值范围内
     */
    public void addSlideRect(EffectModel model){
//        float duration = 0;
////        for (int i = 0; i< colorLumps.size() ; i++){
////            duration += colorLumps.get(i).rect.right - colorLumps.get(i).rect.left;
////        }
////        if (duration >= measuredWidth - paddingRight){
////            // 当前添加的rect总长度已经大于等于控件宽，拒绝再添加
////            return;
////        }
        int left = sliderRect.left;
        int right = sliderRect.left;
//        if (colorLumps.size() > 0) {
//            left = colorLumps.get(colorLumps.size() - 1).rect.right;
//            right = left;
//        }
        Rect rect = new Rect(left, backgroudRect.top, right, backgroudRect.bottom);
        currentRect = new SliderRect(rect, model);
        colorLumps.add(currentRect);
        // 1. 开始的点 当前位置
//        sliderRect.left
        // 2. 长度 length
        // 3. 长度超过后使用最长的 mMaxLength(measureWidth - paddingRight)

    }
    /**
     * length 是进度的长度，单位不是像素，是当前设置的最大值范围内
     */
    public void addSlideRectFromLast(EffectModel model){
//        float duration = 0;
////        for (int i = 0; i< colorLumps.size() ; i++){
////            duration += colorLumps.get(i).rect.right - colorLumps.get(i).rect.left;
////        }
////        if (duration >= measuredWidth - paddingRight){
////            // 当前添加的rect总长度已经大于等于控件宽，拒绝再添加
////            return;
////        }
        int left = sliderRect.right;
        int right = sliderRect.right;
//        if (colorLumps.size() > 0) {
//            left = colorLumps.get(colorLumps.size() - 1).rect.right;
//            right = left;
//        }
        Rect rect = new Rect(left, backgroudRect.top, right, backgroudRect.bottom);
        currentRect = new SliderRect(rect, model);
        colorLumps.add(currentRect);
        // 1. 开始的点 当前位置
//        sliderRect.left
        // 2. 长度 length
        // 3. 长度超过后使用最长的 mMaxLength(measureWidth - paddingRight)

    }

    private class SliderRect {
        public SliderRect(Rect rect, EffectModel model) {
            this.rect = rect;
            this.model = model;
        }

        Rect rect;
        EffectModel model;

        @Override
        public String toString() {
            return "SliderRect{" +
                    "rect=" + rect +
                    ", model=" + model +
                    '}';
        }
    }

    /**
     * 移除最近一个色块
     */
    public void removeLastSliderRect(){
        if (colorLumps.size() > 0){
            int index = colorLumps.size() - 1;
            SliderRect rect = colorLumps.get(index);
            sliderRect.left = rect.rect.left;
            sliderRect.right = rect.rect.left + this.right;
            colorLumps.remove(index);
            invalidate();
        }
    }

    /**
     * 移除最近一个色块，反方向
     */
    public void removeLastSliderRectFromLast(){
        if (colorLumps.size() > 0){
            int index = colorLumps.size() - 1;
            SliderRect rect = colorLumps.get(index);
            sliderRect.right = rect.rect.right;
            sliderRect.left = rect.rect.right - this.right;
            colorLumps.remove(index);
            invalidate();
        }
    }

    public void clearCurrentSlideRect(){
        for (int i = 0; i < colorLumps.size(); i++) {
            Log.e("moose", colorLumps.get(i).toString());
        }
        currentRect = null;
    }

    /**
     * 添加当前视频所有的特效
     * @param effects
     */
    public void addAllSlideRect(ArrayList<Effect> effects){
        colorLumps.clear();
        for (int i = 0; i < effects.size(); i++) {
            Effect effect = effects.get(i);
            float left = effect.startTime * measuredWidth / mMaxLength;
            float right = (effect.startTime + effect.duration) * measuredWidth / mMaxLength;
            Rect rect = new Rect((int) left, backgroudRect.top, (int) right, backgroudRect.bottom);
            SliderRect e = new SliderRect(rect, effect.model);
            colorLumps.add(e);
        }
        if (colorLumps.size() > 0){
            SliderRect rect = colorLumps.get(colorLumps.size() - 1);
            sliderRect.left = rect.rect.right - this.right;
            sliderRect.right = rect.rect.right;
            if (rect.rect.right >= measuredWidth - paddingRight){
                sliderRect.right = measuredWidth - paddingRight;
                sliderRect.left = sliderRect.right - this.right;
            }
        }
        invalidate();
    }

    public void setProgress(float progresss){
        try {
            float left = progresss / mMaxLength * measuredWidth;
            float right = left + this.right;
            sliderRect.right = (int) right;
            sliderRect.left = (int) left;
            Log.e("moose", "progress=" + progresss + ", set Progress, right=" + right + ", left=" +left);
            invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取范围中开始的头
     */
    public float getStartProgress(){
        return sliderRect.left * mMaxLength / measuredWidth;
    }
    /**
     * 获取范围中结束的头
     */
    public float getEndProgress(){
        return sliderRect.right * mMaxLength / measuredWidth;
    }

    public void setListener(OnChooseListener l) {
        this.listener = l;
    }

    public interface OnChooseListener {
        /**
         * 按下控件
         * @param position
         */
        void onSlideStart(float position);

        void onSliding(float start, float end);

        void onSlideEnd(float start, float end);

    }
}
