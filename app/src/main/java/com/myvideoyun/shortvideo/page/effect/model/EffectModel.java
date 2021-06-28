package com.myvideoyun.shortvideo.page.effect.model;

import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageShortVideoFilter;
import com.myvideoyun.shortvideo.page.record.view.StylePlane;

/**
 * Created by 汪洋 on 2019/2/11.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class EffectModel {
    public String thumbnail;
    public String text;
    public String path;
    public int color;
    public MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE type;
    // 特效类型：0 为滤镜特效；1 为时间特效
    public int effectType;

    @Override
    public String toString() {
        return "EffectModel{" +
                "thumbnail='" + thumbnail + '\'' +
                ", text='" + text + '\'' +
                ", path='" + path + '\'' +
                ", color=" + color +
                ", type=" + type +
                ", effectType=" + effectType +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StylePlane && obj.toString().equals(this.toString())) {
            return true;
        }
        return super.equals(obj);
    }
}
