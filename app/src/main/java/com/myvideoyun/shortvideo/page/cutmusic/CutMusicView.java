package com.myvideoyun.shortvideo.page.cutmusic;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.customUI.FrameRangeView;
import com.myvideoyun.shortvideo.MVYPreviewView;

/**
 * Created by yangshunfa on 2019/3/6.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class CutMusicView extends FrameLayout {

    public static final String TAG = "CutView";
    View containerView;

    CutViewCallback callback;
    public MVYPreviewView previewView;
    public FrameRangeView mRangeView;
    private int max = 100;

    public CutMusicView(Context context) {
        super(context);

        setupView();
    }

    private void setupView() {
        containerView = inflate(getContext(), R.layout.activity_cut_music, this);
        mRangeView = findViewById(R.id.rangeView);
        mRangeView.setListener(new FrameRangeView.OnChooseListener() {
            @Override
            public void onSlideStart(float position) {
                Log.d(TAG, "start=" + position);
                if (callback != null) callback.cutChoiseStart(position);
            }

            @Override
            public void onSliding(float start, float end) {
                Log.d(TAG, "start=" + start + " end=" + end);
                if (callback != null) callback.cutDragging(start, end);
            }

            @Override
            public void onSlideEnd(float start, float end) {
                if (callback != null) callback.cutChoiseEnd(start, end);
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
