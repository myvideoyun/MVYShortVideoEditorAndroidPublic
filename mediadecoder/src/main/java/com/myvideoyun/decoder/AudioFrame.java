package com.myvideoyun.decoder;

public class AudioFrame {
    /*标记 -1 表示 eof, -2 表示主要停止*/
    public int flag;

    /*显示时间*/
    public double pts;

    /*显示时长*/
    public double duration;

    /*视频总时长*/
    public double length;

    /*采样率*/
    public int sampleRate;

    /*通道数*/
    public int channels;

    /*数据*/
    public byte[] buffer;

    /*数据大小*/
    public int bufferSize;

    /*全局视频长度*/
    public double globalLength;

    /*全局pts*/
    public double globalPts;

}
