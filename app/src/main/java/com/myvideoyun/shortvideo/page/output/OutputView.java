package com.myvideoyun.shortvideo.page.output;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.customUI.RadioGroup;
import com.myvideoyun.shortvideo.MVYPreviewView;

/**
 * Created by 汪洋 on 2019/2/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class OutputView extends FrameLayout implements View.OnClickListener {

    View containerView;
    MVYPreviewView preview;
    TextView titleTv;
    ImageButton backBt;
    TextView resolutionTv;
    RadioGroup resolutionRadioGroup;
    View line;
    TextView videoBitrateTv;
    RadioGroup videoBitrateRadioGroup;
    View line1;
    TextView audioBitrateTv;
    RadioGroup audioBitrateRadioGroup;
    View line2;
    Button saveBt;

    OutputViewCallback callback;
    View duetBtn;

    public OutputView(Context context) {
        super(context);

        setupView();
    }

    private void setupView() {
        containerView = inflate(getContext(), R.layout.activity_output, this);
        preview = findViewById(R.id.output_preview);
        titleTv = findViewById(R.id.output_title_tv);
        backBt = findViewById(R.id.output_back_bt);
        resolutionTv = findViewById(R.id.output_resolution_tv);
        resolutionRadioGroup = findViewById(R.id.output_resolution_radio_group);
        line = findViewById(R.id.output_line);
        videoBitrateTv = findViewById(R.id.output_video_bitrate_tv);
        videoBitrateRadioGroup = findViewById(R.id.output_video_bitrate_radio_group);
        line1 = findViewById(R.id.output_line1);
        audioBitrateTv = findViewById(R.id.output_audio_bitrate_tv);
        audioBitrateRadioGroup = findViewById(R.id.output_audio_bitrate_radio_group);
        line2 = findViewById(R.id.output_line2);
        saveBt = findViewById(R.id.output_save_bt);
        duetBtn = findViewById(R.id.edit_in_step_shoot);

        duetBtn.setOnClickListener(this);

        titleTv.setText("保存参数");
        titleTv.setTextColor(Color.BLACK);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleTv.setGravity(Gravity.CENTER);

        backBt.setImageResource(R.drawable.selector_output_back_button);
        backBt.setBackgroundColor(Color.TRANSPARENT);
        backBt.setScaleType(ImageView.ScaleType.CENTER);

        resolutionTv.setText("分辨率");
        resolutionTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        resolutionTv.setTextColor(Color.GRAY);

        resolutionRadioGroup.setupView(new String[]{"540p","720p","1080p"});
        resolutionRadioGroup.setSelectedText("720p");

        line.setBackgroundColor(Color.GRAY);

        videoBitrateTv.setText("视频码率\n(kbps)");
        videoBitrateTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        videoBitrateTv.setTextColor(Color.GRAY);

        videoBitrateRadioGroup.setupView(new String[]{"2048","4096","8192"});
        videoBitrateRadioGroup.setSelectedText("4096");

        line1.setBackgroundColor(Color.GRAY);

        audioBitrateTv.setText("音频帧率\n(kbps)");
        audioBitrateTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        audioBitrateTv.setTextColor(Color.GRAY);

        audioBitrateRadioGroup.setupView(new String[]{"64","128","256"});
        audioBitrateRadioGroup.setSelectedText("64");

        line2.setBackgroundColor(Color.GRAY);

        saveBt.setText("保存到相册");
        saveBt.setTextColor(Color.WHITE);
        saveBt.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
        saveBt.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == saveBt) {
            if (callback != null) {
                callback.outputViewOnSave();
            }
        }
        if (v == duetBtn) {
            if (callback != null) {
                callback.duetShoot();
            }
        }
    }
}