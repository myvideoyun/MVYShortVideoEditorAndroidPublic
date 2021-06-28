package com.myvideoyun.shortvideo.page.cut;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.customUI.FrameHorizontalView;
import com.myvideoyun.shortvideo.customUI.FrameSeekView;
import com.myvideoyun.shortvideo.customUI.FrameHorizontalView;
import com.myvideoyun.shortvideo.customUI.FrameSeekView;

/**
 * Created by yangshunfa on 2019/3/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class CutView extends FrameLayout {

    public static final String TAG = "CutView";
    View containerView;

    CutViewCallback callback;
    public SurfaceView previewView;
    public FrameSeekView mRangeView;
    FrameHorizontalView horizontalView;

    public CutView(Context context) {
        super(context);

        setupView();
    }

    private void setupView() {
        containerView = inflate(getContext(), R.layout.activity_cut, this);
        mRangeView = findViewById(R.id.rangeView);
        horizontalView = findViewById(R.id.frame_horizontal_view);
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
        findViewById(R.id.cut_back_bt).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) callback.backwards();
            }
        });
        findViewById(R.id.cut_next_bt).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) callback.next();
            }
        });
        previewView = findViewById(R.id.cut_preview);

    }

}
