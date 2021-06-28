package com.myvideoyun.shortvideo.page.record.model;

import java.io.Serializable;

/**
 * Created by 汪洋 on 2019/2/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class MediaInfoModel implements Serializable {
    // 视频路径
    public String videoPath;

    // 音频路径
    public String audioPath;

    // 音频时长
    public float videoSeconds;

    // 剪切的开始时间
    public float start;

    // 剪切的结束时间
    public float end;

    // 视频宽
    public long width;
    // 视频高
    public long height;

    // 视频比例
    public double ratio;

    @Override
    public String toString() {
        return "MediaInfoModel{" +
                "videoPath='" + videoPath + '\'' +
                ", audioPath='" + audioPath + '\'' +
                ", videoSeconds=" + videoSeconds +
                ", start=" + start +
                ", end=" + end +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
