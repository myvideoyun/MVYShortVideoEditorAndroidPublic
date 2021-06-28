package com.myvideoyun.shortvideo.page.input;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.customUI.MotionLayout;
import com.myvideoyun.shortvideo.customUI.RadioGroup;

/**
 * Created by 汪洋 on 2019/2/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class InputView extends FrameLayout {

    View containerView;

    InputViewCallback callback;

    TextView titleTv;
    TextView resolutionTv;
    RadioGroup resolutionRadioGroup;
    View line;
    TextView frameRateTv;
    RadioGroup frameRateRadioGroup;
    View line1;
    TextView videoBitrateTv;
    RadioGroup videoBitrateRadioGroup;
    View line2;
    TextView audioBitrateTv;
    RadioGroup audioBitrateRadioGroup;
    View line3;
    TextView screenRateTv;
    RadioGroup screenRateRadioGroup;
    View line4;
    Button recordBt;
    Button importBt;
    Button importImageBt;

    public InputView(Context context) {
        super(context);

        setupView();
    }

    private void setupView() {
        containerView = inflate(getContext(), R.layout.activity_input, this);

        titleTv = findViewById(R.id.input_title_tv);
        resolutionTv = findViewById(R.id.input_resolution_tv);
        resolutionRadioGroup = findViewById(R.id.input_resolution_radio_group);
        line = findViewById(R.id.input_line);
        frameRateTv = findViewById(R.id.input_frame_rate_tv);
        frameRateRadioGroup = findViewById(R.id.input_frame_rate_radio_group);
        line1 = findViewById(R.id.input_line1);
        videoBitrateTv = findViewById(R.id.input_video_bitrate_tv);
        videoBitrateRadioGroup = findViewById(R.id.input_video_bitrate_radio_group);
        line2 = findViewById(R.id.input_line2);
        audioBitrateTv = findViewById(R.id.input_audio_bitrate_tv);
        audioBitrateRadioGroup = findViewById(R.id.input_audio_bitrate_radio_group);
        line3 = findViewById(R.id.input_line3);
        screenRateTv = findViewById(R.id.input_screen_rate_tv);
        screenRateRadioGroup = findViewById(R.id.input_screen_rate_radio_group);
        line4 = findViewById(R.id.input_line4);

        recordBt = findViewById(R.id.input_record_bt);
        importImageBt = findViewById(R.id.input_import_image_bt);
        importBt = findViewById(R.id.input_import_bt);

        titleTv.setText("录制参数");
        titleTv.setTextColor(Color.BLACK);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleTv.setGravity(Gravity.CENTER);

        resolutionTv.setText("分辨率");
        resolutionTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        resolutionTv.setTextColor(Color.GRAY);

        resolutionRadioGroup.setupView(new String[]{"540p","720p","1080p"});
        resolutionRadioGroup.setSelectedText("720p");

        line.setBackgroundColor(Color.GRAY);

        frameRateTv.setText("帧率(fps)");
        frameRateTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        frameRateTv.setTextColor(Color.GRAY);

        frameRateRadioGroup.setupView(new String[]{"15","24","30"});
        frameRateRadioGroup.setSelectedText("30");

        line1.setBackgroundColor(Color.GRAY);

        videoBitrateTv.setText("视频码率\n(kbps)");
        videoBitrateTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        videoBitrateTv.setTextColor(Color.GRAY);

        videoBitrateRadioGroup.setupView(new String[]{"2048","4096","8192"});
        videoBitrateRadioGroup.setSelectedText("4096");

        line2.setBackgroundColor(Color.GRAY);

        audioBitrateTv.setText("音频帧率\n(kbps)");
        audioBitrateTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        audioBitrateTv.setTextColor(Color.GRAY);

        audioBitrateRadioGroup.setupView(new String[]{"64","128","256"});
        audioBitrateRadioGroup.setSelectedText("64");

        line3.setBackgroundColor(Color.GRAY);

        screenRateTv.setText("视频比例");
        screenRateTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        screenRateTv.setTextColor(Color.GRAY);

        screenRateRadioGroup.setupView(new String[]{"16:9","4:3","1:1"});
        screenRateRadioGroup.setSelectedText("16:9");

        line4.setBackgroundColor(Color.GRAY);

        recordBt.setText("开始录制");
        recordBt.setTextColor(getResources().getColor(android.R.color.black));
        recordBt.setPadding(0,0,0,0);
        recordBt.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.selector_input_record_button), null, null);
        recordBt.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5.f, getResources().getDisplayMetrics()));
        recordBt.setBackgroundColor(Color.TRANSPARENT);

        recordBt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.inputViewOnRecordBtClick(v);
                }
            }
        });

        importImageBt.setText("导入图片");
        importImageBt.setTextColor(getResources().getColor(android.R.color.black));
        importImageBt.setPadding(0,0,0,0);
        importImageBt.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.selector_input_picture_button), null, null);
        importImageBt.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5.f, getResources().getDisplayMetrics()));
        importImageBt.setBackgroundColor(Color.TRANSPARENT);

        importImageBt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.inputViewOnImportImageBtClick(v);
                }
            }
        });

        importBt.setText("本地导入");
        importBt.setTextColor(getResources().getColor(android.R.color.black));
        importBt.setPadding(0,0,0,0);
        importBt.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.selector_input_picture_button), null, null);
        importBt.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5.f, getResources().getDisplayMetrics()));
        importBt.setBackgroundColor(Color.TRANSPARENT);

        importBt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.inputViewOnImportBtClick(v);
                }
            }
        });
    }
}

