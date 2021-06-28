package com.myvideoyun.shortvideo.model;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageStickerFilter;

import java.io.Serializable;

public class StickerModel implements Serializable {
    public boolean isAdded = false;
    // bitmap 路径
    public String path;
    // 在EditActivity 中与贴纸映射的view
    public View view;
    // 开始时间
    public float startTime;
    // 结束时间
    public float endTime;
    // 原始的matrix
    public float[] originalMatrix;
    //贴纸显示的x点
    public float x = 0f;
    // 贴纸显示的y点
    public float y = 0f;
    // 贴纸缩放比例
    public float scale = 1f;

    // 加入到handler 中的
    public MVYGPUImageStickerFilter.MVYGPUImageStickerModel imageStickerModel;

    public void recycleView(){
        if (view != null){
            ViewParent parent = view.getParent();
            if (parent != null && parent instanceof ViewGroup){
                ((ViewGroup)parent).removeView(view);
            }
            view = null;
        }
    }

    @Override
    public String toString() {
        return "StickerModel{" +
                "path='" + path + '\'' +
                ", view=" + view +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", imageStickerModel=" + imageStickerModel +
                '}';
    }
}
