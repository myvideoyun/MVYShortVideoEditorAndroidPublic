package com.myvideoyun.decoder;

/**
 * Created by 汪洋 on 2019/2/2.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class VideoFrame {

    /*标记 -1 表示 eof, -2 表示主要停止*/
    public int flag;

    /*视频宽*/
    public int width;

    /*视频高*/
    public int height;

    /*行宽*/
    public int lineSize;

    /*旋转信息*/
    public int rotate;

    /*显示时间*/
    public double pts;

    /*显示时长*/
    public double duration;

    /*视频总时长*/
    public double length;

    /*y数据*/
    public byte[] yData;

    /*u数据*/
    public byte[] uData;

    /*v数据*/
    public byte[] vData;

    /*是否是关键帧*/
    public int isKeyFrame;

    /*全局视频长度*/
    public double globalLength;

    /*全局pts*/
    public double globalPts;

    public VideoFrame() {
    }

    @Override
    public String toString() {
        return "VideoFrame{" +
                "width=" + width +
                ", height=" + height +
                ", lineSize=" + lineSize +
                ", rotate=" + rotate +
                ", pts=" + pts +
                ", duration=" + duration +
                ", length=" + length +
                ", yData=" + yData.length +
                ", uData=" + uData.length +
                ", vData=" + vData.length +
                ", isKeyFrame=" + isKeyFrame +
                '}';
    }
}
