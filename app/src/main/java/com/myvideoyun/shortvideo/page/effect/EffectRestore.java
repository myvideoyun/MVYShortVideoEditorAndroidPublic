package com.myvideoyun.shortvideo.page.effect;

import com.myvideoyun.decoder.VideoFrame;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageShortVideoFilter;
import com.myvideoyun.shortvideo.MVYVideoEffectHandler;
import com.myvideoyun.shortvideo.page.effect.model.Effect;

import java.util.ArrayList;

/**
 * Created by 汪洋 on 2019/3/30.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 * 用于储存全局的视频特效
 */
public class EffectRestore {

    /**
     * 是否翻转特效，默认 false ，正向解码
     */
    public static boolean isReverse = false;
    public static boolean isFast = false;
    public static boolean isSlow = false;
    public static boolean isNormal = true;
    public static ArrayList<Effect> effects = new ArrayList<>();
    public static String reverseVideoPaths = "";
    public static void setEffectHandlerType(MVYVideoEffectHandler effectHandler, VideoFrame frame,  Effect tempEffect){
        if (effects.size() > 0) {
            for (int i = effects.size() - 1; i >= 0; i--) {
                Effect effect = effects.get(i);
                if (frame.pts >= effect.startTime && frame.pts < effect.startTime + effect.duration) {
                    effectHandler.setTypeOfShortVideo(effect.model.type);
                    break;
                } else {
                    effectHandler.setTypeOfShortVideo(MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_NONE);
                }
            }
        } else {
            effectHandler.setTypeOfShortVideo(MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_NONE);
        }
        if (tempEffect != null){
            effectHandler.setTypeOfShortVideo(tempEffect.model.type);
        }
    }
}
