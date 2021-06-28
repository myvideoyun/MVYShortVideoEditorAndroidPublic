package com.myvideoyun.shortvideo.page.music.model;

import java.io.Serializable;

public class MediaModel implements Serializable {

    // 标题
    public String title =  "";

    // 路径
    public String path = "";

    // 剪切路径
    public String cutPath = "";

    // 是否选中
    public boolean checked = false;

    // 时长
    public long duration;

    // 高
    public long height;

    // 宽
    public long width;

    // 文件大小
    public long size;

    // 显示名称
    public String diaplayName;

    // 缩略图
    public String thumPath;

    // 开始时间
    public float start;

    // 结束时间
    public float end;

    // 比例
    public float ratio;

    // 图片路径
    public String imagePath = "";

    // 图片方向
    public int orientation;
}
