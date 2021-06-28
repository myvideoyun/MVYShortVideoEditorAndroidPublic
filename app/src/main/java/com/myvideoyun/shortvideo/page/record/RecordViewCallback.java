package com.myvideoyun.shortvideo.page.record;

import com.myvideoyun.shortvideo.page.record.model.StyleModel;

public interface RecordViewCallback {
    void recordViewStartRecord();
    void recordViewStopRecord();
    void switchCamera();
    void switchCameraFlash();

    void recordViewAlterSpeedRate(int index);

    void recordViewRenderNewStyle(StyleModel model);

    void choiceMusic();

    void nextToEditor();

    /**
     * 设置滤镜强度范围
     * @param intensityMax
     * @param progress
     */
    void setIntensityBeauty(float intensityMax, float progress);

    /**
     * 删除按钮点击事件
     */
    void removeRecordedVideoFromLast();

    /**
     * 拍照
     */
    void takePicture();

    /**
     * 设置亮度
     * @param max
     * @param progress
     */
    void setIntensityBrightness(float max, float progress);

    /**
     * 设置饱和度
     * @param max
     * @param progress
     */
    void setIntensitySaturability(float max, float progress);

    void duetShoot();
}
