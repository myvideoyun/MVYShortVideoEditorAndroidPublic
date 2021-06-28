package com.myvideoyun.shortvideo.page.record;

import android.content.Context;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.page.record.model.StyleModel;
import com.myvideoyun.shortvideo.page.record.view.ProgressView;
import com.myvideoyun.shortvideo.page.record.view.SpeedRadioLayout;
import com.myvideoyun.shortvideo.page.record.view.StylePlane;
import com.myvideoyun.shortvideo.page.record.view.StylePlaneOnHideListener;
import com.myvideoyun.shortvideo.MVYPreviewView;

/**
 * Created by 汪洋 on 2019/2/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class RecordView extends FrameLayout implements View.OnClickListener, StylePlaneOnHideListener, SpeedRadioLayout.OnClickTabListener, StylePlane.StylePlaneOnClickItemListener {

    RecordViewCallback callback;

    View containerView;
    MVYPreviewView cameraPreview;
    // 合拍 view
    SurfaceView duetPreview;
    ImageButton backButton;
    ImageButton switchCameraButton;
    ViewGroup bottomContainerLayout;
    Button nextButton;
    ImageButton styleButton;
    CheckBox beautyButton;
    CheckBox flashButton;
    ImageButton musicButton;
    SpeedRadioLayout speedRadioLayout;
    View progressBG;
    ProgressView progressView;
    ImageButton recordButton;
    View rightLayout;
    ImageButton removeVideoItemButton;

    StylePlane stylePlane;
    private SeekBar beautySb;
    private SeekBar brightnessSb;
    private SeekBar saturabilitySb;
    private int intensityMax = 100;
    private View hideCl;
    private ConstraintLayout rootCl;
    private ImageButton shootBtn;
    ImageView previewImageView;
    private View duetBtn;

    public RecordView(Context context) {
        super(context);
        setupView();
    }

    public RecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupView();
    }

    public RecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupView();
    }

    protected void setupView() {
        containerView = inflate(getContext(), R.layout.activity_record, this);

        cameraPreview = findViewById(R.id.record_camera_preview);
        backButton = findViewById(R.id.record_back_bt);
        switchCameraButton = findViewById(R.id.record_switch_camera_bt);
        bottomContainerLayout = findViewById(R.id.record_container_layout);
        nextButton = findViewById(R.id.record_next_bt);
        styleButton = findViewById(R.id.record_style_bt);
        beautyButton = findViewById(R.id.record_beauty_bt);
        flashButton = findViewById(R.id.record_flash_bt);
        musicButton = findViewById(R.id.record_music_bt);
        speedRadioLayout = findViewById(R.id.record_speed_radio_layout);
        progressBG = findViewById(R.id.record_progress_bg);
        progressView = findViewById(R.id.record_progress_view);
        recordButton = findViewById(R.id.record_record_bt);
        duetPreview = findViewById(R.id.edit_record_preview);
        rightLayout = findViewById(R.id.record_button_right_layout);
        removeVideoItemButton = findViewById(R.id.record_remove_video_item_bt);
        stylePlane = findViewById(R.id.record_style_plane_view);
        beautySb = findViewById(R.id.record_intensity_sb);
        brightnessSb = findViewById(R.id.record_brightness_sb);
        saturabilitySb = findViewById(R.id.record_saturability_sb);
        hideCl = findViewById(R.id.hiderCl);
        rootCl = findViewById(R.id.rootView);
        shootBtn = findViewById(R.id.record_shoot_ib);
        previewImageView = findViewById(R.id.record_img_iv);
        duetBtn = findViewById(R.id.edit_in_step_shoot);


        duetBtn.setOnClickListener(this);

        shootBtn.setOnClickListener(this);

        backButton.setImageResource(R.drawable.selector_record_back_button);
        backButton.setBackgroundColor(Color.TRANSPARENT);

        switchCameraButton.setImageResource(R.drawable.selector_record_switch_camera_button);
        switchCameraButton.setBackgroundColor(Color.TRANSPARENT);
        switchCameraButton.setOnClickListener(this);

        flashButton.setOnClickListener(this);
        musicButton.setOnClickListener(this);

        nextButton.setText("下一步");
        nextButton.setTextColor(Color.WHITE);
        nextButton.setBackgroundColor(Color.TRANSPARENT);
        nextButton.setOnClickListener(this);

        styleButton.setImageResource(R.drawable.selector_record_style_button);
        styleButton.setBackgroundColor(Color.TRANSPARENT);
        styleButton.setOnClickListener(this);
        stylePlane.setOnClickItemListener(this);

//        beautyButton.setImageResource(R.drawable.selector_record_beauty_button);
//        beautyButton.setBackgroundColor(Color.TRANSPARENT);
        beautyButton.setOnClickListener(this);

        speedRadioLayout.setSelectedText("标准");
        speedRadioLayout.setOnClickTabListener(this);

        recordButton.setImageResource(R.drawable.selector_record_record_button);
        recordButton.setBackgroundColor(Color.TRANSPARENT);
        recordButton.setOnClickListener(this);

        removeVideoItemButton.setImageResource(R.drawable.selector_record_delete_item_button);
        removeVideoItemButton.setBackgroundColor(Color.TRANSPARENT);
        removeVideoItemButton.setOnClickListener(this);

        hideCl.setOnClickListener(this);

        stylePlane.hideListener = this;

        beautySb.setMax(intensityMax);
        beautySb.setProgress(8);
        beautySb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (callback != null) callback.setIntensityBeauty(intensityMax, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        brightnessSb.setMax(200);
        brightnessSb.setProgress(100);
        brightnessSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (callback != null) callback.setIntensityBrightness(brightnessSb.getMax(), progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        saturabilitySb.setMax(200);
        saturabilitySb.setProgress(83);// 10 / 11
        saturabilitySb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (callback != null) callback.setIntensitySaturability(saturabilitySb.getMax(), progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        beautyButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    hideCl.setVisibility(VISIBLE);
                    onStylePlaneHide(false);
                } else {
                    hideCl.setVisibility(GONE);
                    onStylePlaneHide(true);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v == styleButton) {
            stylePlane.hideUseAnim(false);
        }else if (v == recordButton) {
            if (!v.isSelected()) {
                Log.d("wangyang", "开始录制");
                if (callback != null) {
                    showDialogView(false);
                    callback.recordViewStartRecord();
                }
            } else {
                Log.d("wangyang", "结束录制");
                showDialogView(true);
                if (callback != null) {
                    callback.recordViewStopRecord();
                }
            }
            v.setSelected(!v.isSelected());
        } else if (v == switchCameraButton){
            if (callback != null) callback.switchCamera();
        } else if (v == flashButton){
            if (callback != null){
                callback.switchCameraFlash();
            }
//            if (flashButton.isChecked()) flashButton.setText("开");
//            else flashButton.setText("关");
        } else if (v == musicButton){
            if (callback != null) callback.choiceMusic();
        }  else if (v == shootBtn){
            if (callback != null) callback.takePicture();
        } else if (v == nextButton){
            if (callback != null) callback.nextToEditor();
        } else if (v == hideCl){
            hideCl.setVisibility(GONE);
            beautyButton.setChecked(false);
        } else if (v == removeVideoItemButton){
            if (callback != null) callback.removeRecordedVideoFromLast();
        } else if (v == duetBtn){
            if (callback != null) callback.duetShoot();
        }
    }

    @Override
    public void onStylePlaneHide(Boolean hide) {
        if (hide) {
            backButton.setVisibility(View.VISIBLE);
            switchCameraButton.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.VISIBLE);
            styleButton.setVisibility(View.VISIBLE);
            beautyButton.setVisibility(View.VISIBLE);
            progressBG.setVisibility(View.VISIBLE);
            progressView.setVisibility(View.VISIBLE);
            recordButton.setVisibility(View.VISIBLE);
            speedRadioLayout.setVisibility(View.VISIBLE);
            rightLayout.setVisibility(View.VISIBLE);
            flashButton.setVisibility(View.VISIBLE);
            musicButton.setVisibility(View.VISIBLE);
            shootBtn.setVisibility(View.VISIBLE);
        } else {
            backButton.setVisibility(View.INVISIBLE);
            switchCameraButton.setVisibility(View.INVISIBLE);
            nextButton.setVisibility(View.INVISIBLE);
            styleButton.setVisibility(View.INVISIBLE);
            beautyButton.setVisibility(View.INVISIBLE);
            progressBG.setVisibility(View.INVISIBLE);
            progressView.setVisibility(View.INVISIBLE);
            recordButton.setVisibility(View.INVISIBLE);
            speedRadioLayout.setVisibility(View.INVISIBLE);
            rightLayout.setVisibility(View.INVISIBLE);
            flashButton.setVisibility(View.INVISIBLE);
            musicButton.setVisibility(View.INVISIBLE);
            shootBtn.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onClick(int index) {
        if (callback != null){
            callback.recordViewAlterSpeedRate(index);
        }
    }

    @Override
    public void onClickItem(int position, StyleModel model) {
        if (callback != null){
            callback.recordViewRenderNewStyle(model);
        }
    }


    /**
     * 控制底部浮窗是否显示，同时控制除底部浮窗外的其他控件（排除预览）
     * @param isShow
     */
    private void showDialogView(boolean isShow) {
        int visibility = isShow ? VISIBLE : INVISIBLE;
        speedRadioLayout.setVisibility(visibility);
        bottomContainerLayout.setVisibility(visibility);
        rightLayout.setVisibility(visibility);

    }
}

