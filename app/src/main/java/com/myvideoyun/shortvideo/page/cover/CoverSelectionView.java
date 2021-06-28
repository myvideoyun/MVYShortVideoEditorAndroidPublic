package com.myvideoyun.shortvideo.page.cover;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.customUI.FrameRangeView;
import com.myvideoyun.shortvideo.MVYPreviewView;

/**
 * Created by yangshunfa on 2019/3/9.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class CoverSelectionView extends FrameLayout implements FrameRangeView.OnChooseListener {

    public static final String TAG = "CutView";
    View containerView;

    CoverSelectionViewCallback callback;
    public MVYPreviewView previewView;
    ConstraintLayout previewRootView;
    FrameRangeView rangeView;
    ImageView coverIv;
//    ImageView coverIv;

    public CoverSelectionView(Context context) {
        super(context);

        setupView();
    }

    private void setupView() {
        containerView = inflate(getContext(), R.layout.activity_cover, this);
        previewRootView = findViewById(R.id.previewRootView);
//        coverIv = findViewById(R.id.coverIv);
        rangeView = findViewById(R.id.cover_range_view);
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
        coverIv = findViewById(R.id.coverIv);
        // set view
        rangeView.setMaxLength(1500);
        rangeView.setMinLength(10);
        rangeView.setListener(this);

    }

    @Override
    public void onSlideStart(float position) {
        if (callback != null) callback.cutChoiseStart(position);
    }

    @Override
    public void onSliding(float start, float end) {
        if (callback != null) callback.dragging(start, end);
    }

    @Override
    public void onSlideEnd(float start, float end) {
        if (callback != null) callback.cutFinish(start, end);
    }
}
