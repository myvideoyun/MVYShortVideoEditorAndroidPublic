package com.myvideoyun.shortvideo.page.effect.model;

import java.io.Serializable;
/**
 * Created by 汪洋 on 2019/3/30.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class Effect implements Serializable {
    public float startTime;
    public float duration;
    public EffectModel model;

    @Override
    public String toString() {
        return "Effect{" +
                "startTime=" + startTime +
                ", duration=" + duration +
                ", model=" + model +
                '}';
    }
}
