package com.myvideoyun.shortvideo.page.effect;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.customUI.FrameRangeView;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageShortVideoFilter;
import com.myvideoyun.shortvideo.page.common.EffectHorizontalView;
import com.myvideoyun.shortvideo.page.cover.CoverSelectionViewCallback;
import com.myvideoyun.shortvideo.page.effect.model.EffectModel;
import com.myvideoyun.shortvideo.MVYPreviewView;

import java.io.IOException;

/**
 * Created by 汪洋 on 2019/2/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class EffectView extends FrameLayout implements FrameRangeView.OnChooseListener {

    public static final String TAG = "EffectView";
    View containerView;

    CoverSelectionViewCallback callback;
    public MVYPreviewView previewView;
    ConstraintLayout previewRootView;
    FrameRangeView rangeView;
    ImageView coverIv;
    EffectHorizontalView effetListView;
    Button withdrawBtn;
    private EffectModel[] filterEffects;
    private EffectModel[] timeEffects;
    private RadioGroup effectRg;
    //    ImageView coverIv;

    public EffectView(Context context) {
        super(context);

        setupView();
    }

    private void setupView() {
        containerView = inflate(getContext(), R.layout.activity_effect, this);
        previewRootView = findViewById(R.id.previewRootView);
//        coverIv = findViewById(R.id.coverIv);
        rangeView = findViewById(R.id.cover_range_view);
        withdrawBtn = findViewById(R.id.withdraw_btn);
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
        effetListView = findViewById(R.id.effet_list_view);
        effectRg = findViewById(R.id.effect_rg);

        // set view
        rangeView.setMaxLength(1500);
        rangeView.setMinLength(1);
        rangeView.setListener(this);
        // 撤销特效按钮
        withdrawBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rangeView.colorLumps.size() > 0){
//                    rangeView.colorLumps.remove(rangeView.colorLumps.size() - 1);
                    if (!EffectRestore.isReverse){
                        rangeView.removeLastSliderRect();
                    } else {
                        rangeView.removeLastSliderRectFromLast();
                    }
                    rangeView.invalidate();
                }
                if (rangeView.colorLumps.size() <= 0){
                    withdrawBtn.setVisibility(INVISIBLE);
                }
                if (EffectRestore.effects.size() > 0){
                    EffectRestore.effects.remove(EffectRestore.effects.size() - 1);
                }
            }
        });
        try {
            filterEffects = styleData(getContext());
            timeEffects = timeData(getContext());
            effetListView.setStyles(filterEffects);
        } catch (IOException e) {
            e.printStackTrace();
        }
        effectRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.effect_filter_rb){
                    effetListView.setStyles(filterEffects);
                } else if (checkedId == R.id.effect_time_rb){
                    effetListView.setStyles(timeEffects);
                }
            }
        });
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

    private static EffectModel[] styleData(Context context) throws IOException {
        String[] filters = context.getResources().getAssets().list("FilterResources/filter");
        String[] icons = context.getResources().getAssets().list("FilterResources/icon");

        EffectModel[] models = new EffectModel[16];
        for (int i=0; i < 16 ; i++) {
            EffectModel styleModel = new EffectModel();
            styleModel.path = "FilterResources/filter/" + filters[i];
            styleModel.thumbnail = "FilterResources/icon/" + icons[i];
            styleModel.text = EffectHorizontalView.effects[i];
            styleModel.color = EffectHorizontalView.colors[i];
            styleModel.type = EffectHorizontalView.types[i];
            styleModel.effectType = 0;
//            styleModel.text = filters[i].substring(2, filters[i].length() - 4);
            models[i] = styleModel;
        }

        return models;
    }

    private static String[] timeEffectTexts = {
            "正常",
            "时光倒流",
            "闪一下",
            "慢动作",
    };
    private static EffectModel[] timeData(Context context) throws IOException {
        String[] filters = context.getResources().getAssets().list("FilterResources/filter");
        String[] icons = context.getResources().getAssets().list("FilterResources/icon");
        EffectModel[] models = new EffectModel[timeEffectTexts.length];

        for (int i=0; i < timeEffectTexts.length ; i++) {
            EffectModel styleModel = new EffectModel();
            styleModel.path = "FilterResources/filter/" + filters[i];
            styleModel.thumbnail = "FilterResources/icon/" + icons[i];
            styleModel.text = timeEffectTexts[i];
            styleModel.color = colors[i];
            styleModel.type = MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_NONE;
            styleModel.effectType = 1;
            models[i] = styleModel;
        }
        return models;
    }

    public static int [] colors = {
            0x00ffffff, 0xfffe44c3, 0xff840ada, 0xff0ada19,
    };
}