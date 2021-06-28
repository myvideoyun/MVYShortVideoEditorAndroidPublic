package com.myvideoyun.shortvideo.page.addpic;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.customUI.FrameSeekView;
import com.myvideoyun.shortvideo.page.record.model.StyleModel;
import com.myvideoyun.shortvideo.page.record.view.StylePlane;
import com.myvideoyun.shortvideo.MVYPreviewView;

/**
 * Created by yangshunfa on 2019/3/9.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class PictureView extends FrameLayout implements StylePlane.StylePlaneOnClickItemListener {

    public static final String TAG = "CutView";
    View containerView;

    PictureViewCallback callback;
    public MVYPreviewView previewView;
    public FrameSeekView mRangeView;
    private StylePlane stickerStylePlane;
    ConstraintLayout previewRootView;

    public PictureView(Context context) {
        super(context);

        setupView();
    }

    private void setupView() {
        containerView = inflate(getContext(), R.layout.activity_picture, this);
        mRangeView = findViewById(R.id.rangeView);
        previewRootView = findViewById(R.id.previewRootView);
        mRangeView.setListener(new FrameSeekView.OnChooseListener() {
            @Override
            public void onSlideStart(float position) {
                Log.d(TAG, "start=" + position);
                if (callback != null) callback.cutChoiseStartPosition(position);
            }

            @Override
            public void onSlideEnd(float position) {
                Log.d(TAG, "end=" + position);
                if (callback != null) callback.cutChoiseEndPosition(position);
            }

            @Override
            public void onChoose(float start, float end) {
                Log.d(TAG, "start=" + start + " end=" + end);
                if (callback != null) callback.cutFinish(start, end);
            }

            @Override
            public void onTouchedSlider() {
                Log.d(TAG, "onTouchedSlider.");
                if (callback != null) callback.cutChoosing();
            }
        });
        findViewById(R.id.back_bt).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) callback.backwards();
            }
        });
        findViewById(R.id.next_bt).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) callback.finishEdit();
            }
        });
        previewView = findViewById(R.id.preview);

        stickerStylePlane = findViewById(R.id.stickerStylePlane);
        stickerStylePlane.setVisibility(VISIBLE);
        stickerStylePlane.setOnClickItemListener(this);
        stickerStylePlane.setOutskirtsHide(false);
    }

    @Override
    public void onClickItem(int position, StyleModel model) {
        if (callback != null){
            callback.addStickerToVideo(model);
        }
    }
}
