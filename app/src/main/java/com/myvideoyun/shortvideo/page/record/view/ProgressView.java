package com.myvideoyun.shortvideo.page.record.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;
import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;

import java.util.ArrayList;

/**
 * Created by 汪洋 on 2019/2/4.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class ProgressView extends FrameLayout {

    public float longestVideoSeconds;
    public MediaInfoModel recordingMedia;
    public ArrayList<MediaInfoModel> medias;

    private Paint orangeColorPaint;
    private Paint whiteColorPaint;
    private Paint mBackgroundPaint;
    private float endRight;
    private float realTimeTotalSecond;

    public ProgressView(Context context) {
        super(context);

        setupView();
    }

    public ProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setupView();
    }

    public ProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setupView();
    }

    private void setupView() {
        setBackgroundColor(Color.TRANSPARENT);

        orangeColorPaint = new Paint();
        orangeColorPaint.setColor(Color.argb(255, 254, 112, 68));
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(getResources().getColor(R.color.grayColor));

        whiteColorPaint = new Paint();
        whiteColorPaint.setColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float percentCount = 0;
        canvas.drawRect(0, 0, getWidth(), getHeight(), mBackgroundPaint);

        for (int x=0; x<medias.size(); x++) {
            MediaInfoModel model = medias.get(x);
            Log.e("moose", "progress view current media count=" + x + " , media length=" + model.videoSeconds);
            float percent = model.videoSeconds / longestVideoSeconds;
            // 绘制已经录制的片段
            endRight = (percent + percentCount) * getWidth();
            canvas.drawRect(percentCount * getWidth(), 0, endRight - 10, getHeight(), orangeColorPaint);
            // 绘制结束的白色间隔
            canvas.drawRect(endRight-10, 0, endRight, getHeight(), whiteColorPaint);

            percentCount += percent;
        }

        // 绘制当前进度——进度条动画
        if (realTimeTotalSecond > 0) {
            float percent = realTimeTotalSecond / longestVideoSeconds;
            // 绘制正在录制的片段
            canvas.drawRect(percentCount * getWidth(), 0, percent * getWidth(), getHeight(), orangeColorPaint);
        }
    }

    /**
     * 设置当前实时的录制进度
     * @param realTimeSecond
     */
    public void setRecordingMedia(float realTimeSecond) {
        this.realTimeTotalSecond = realTimeSecond;
        postInvalidate();
    }
}
