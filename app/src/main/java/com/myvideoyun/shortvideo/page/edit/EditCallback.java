package com.myvideoyun.shortvideo.page.edit;

import android.graphics.Bitmap;

import com.myvideoyun.shortvideo.page.record.model.StyleModel;
import com.myvideoyun.shortvideo.page.record.model.StyleModel;

/**
 * Created by yangshunfa on 2019/3/6.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public interface EditCallback {
    /**
     * 选择音乐按钮触发
     */
    void chooseMusic();

    /**
     * 裁剪音乐按钮触发
     */
    void cutMusic();

    /**
     * 设置原声音量
     * @param max
     * @param progress
     */
    void setOriginalVolume(float max, float progress);

    /**
     * 设置选择的音乐音量
     * @param max
     * @param progress
     */
    void setMusicVolume(float max, float progress);

    /**
     * 添加贴图到视频中
     * @param model
     */
    void addSticker(StyleModel model);

    /**
     * 添加字幕到视频
     * @param bitmap
     */
    void addSubtitle(Bitmap bitmap);

    /**
     * 进入下一个页面（output）
     */
    void goNextOutput();

    /**
     * 切换原声开关
     * @param isChecked
     */
    void switchAudioToOriginal(boolean isChecked);

    /**
     * 选择封面
     */
    void choiseCover();

    /**
     * 进入合拍按钮
     */
    void duetShoot();

    /**
     * 点击特效按钮
     */
    void clickEffect();
}
