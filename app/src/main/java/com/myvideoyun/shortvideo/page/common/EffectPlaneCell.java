package com.myvideoyun.shortvideo.page.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.myvideoyun.shortvideo.R;

import java.util.Calendar;

/**
 * Created by 汪洋 on 2019/2/10.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class EffectPlaneCell extends FrameLayout {
    public View containerView;
    public ImageView imageView;
    public TextView textView;

    public EffectPlaneCell(Context context) {
        super(context);
        setupView();
    }

    public EffectPlaneCell(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupView();
    }

    public EffectPlaneCell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupView();
    }

    private void setupView() {
        containerView = inflate(getContext(), R.layout.effect_plane_cell, this);
        imageView = containerView.findViewById(R.id.style_plane_cell_iv);
        textView = containerView.findViewById(R.id.style_plane_cell_tv);
    }

    private volatile boolean isStopEmit = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                isStopEmit = false;
                if (listener != null) {
                    listener.onStartTouched(Calendar.getInstance().getTimeInMillis());
                }
                startEmit();
//                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
//                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_UP:
//                getParent().requestDisallowInterceptTouchEvent(true);
                isStopEmit = true;
                if (listener != null) {
                    listener.onEndTouched(Calendar.getInstance().getTimeInMillis());
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                isStopEmit = true;
                if (listener != null) {
                    listener.onEndTouched(Calendar.getInstance().getTimeInMillis());
                }
                break;
        }
        return super.onTouchEvent(event);
    }
    private void startEmit(){
        final Runnable action = new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onTouching(Calendar.getInstance().getTimeInMillis());
                }
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isStopEmit) {
                    post(action);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    private OnLongTouchListener listener;
    public void setLongTouchListener(OnLongTouchListener l){
        this.listener = l;
    }

    public interface OnLongTouchListener {
        void onStartTouched(long time);
        void onTouching(long time);
        void onEndTouched(long time);
    }
}
