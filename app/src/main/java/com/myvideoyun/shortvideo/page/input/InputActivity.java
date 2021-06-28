package com.myvideoyun.shortvideo.page.input;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.myvideoyun.shader.MVYLicenseManager;
import com.myvideoyun.shortvideo.page.input.image.ImportImageActivity;
import com.myvideoyun.shortvideo.page.input.video.ImportVideoActivity;
import com.myvideoyun.shortvideo.page.record.RecordActivity;

/**
 * Created by 汪洋 on 2019/1/31.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class InputActivity extends AppCompatActivity implements InputViewCallback {

    public static final String RESOLUTION = "resolution";
    public static final String FRAME_RATE = "frameRate";
    public static final String VIDEO_BITRATE = "videoBitrate";
    public static final String AUDIO_BITRATE = "audioBitrate";
    public static final String SCREEN_RATE = "screenRate";

    InputView inputView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inputView = new InputView(getBaseContext());
        inputView.callback = this;
        setContentView(inputView);

        MVYLicenseManager.InitLicense(getApplicationContext(), "xgLeQ5G6YF0zLOBUH9GQei/BbyFqkW9l+ELnRN+E2vZguMRDG60OvZ13mmD8e8Fg", new MVYLicenseManager.OnResultCallback() {
            @Override
            public void onResult(int ret) {
                Log.d("myvideoyun", "License初始化结果 : " + ret);
            }
        });
    }

    private void enterNextPage() {
        String resolution = inputView.resolutionRadioGroup.getSelectedText();
        String frameRate = inputView.frameRateRadioGroup.getSelectedText();
        String videoBitrate = inputView.videoBitrateRadioGroup.getSelectedText();
        String audioBitrate = inputView.audioBitrateRadioGroup.getSelectedText();
        String screenRate = inputView.screenRateRadioGroup.getSelectedText();

        Intent intent = new Intent(this, RecordActivity.class);
        intent.putExtra(RESOLUTION, resolution);
        intent.putExtra(FRAME_RATE, frameRate);
        intent.putExtra(VIDEO_BITRATE, videoBitrate);
        intent.putExtra(AUDIO_BITRATE, audioBitrate);
        intent.putExtra(SCREEN_RATE, screenRate);

        startActivity(intent);
    }

    @Override
    public void inputViewOnRecordBtClick(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, 1001);
            } else {
                enterNextPage();
            }
        } else {
            enterNextPage();
        }
    }

    @Override
    public void inputViewOnImportBtClick(View v) {
        Intent intent = new Intent(this, ImportVideoActivity.class);
        startActivity(intent);
    }

    @Override
    public void inputViewOnImportImageBtClick(View v) {
        Intent intent = new Intent(this, ImportImageActivity.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            enterNextPage();
        }
    }
}
