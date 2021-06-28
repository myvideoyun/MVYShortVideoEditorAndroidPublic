package com.myvideoyun.shortvideo.GPUImage;

/**
 * Created by 汪洋 on 2018/12/8.
 * Copyright © 2018年 myvideoyun. All rights reserved.
 */
public interface MVYGPUImageInput {
    void setInputSize(int width, int height);
    void setInputFramebuffer(MVYGPUImageFramebuffer newInputFramebuffer);
    void newFrameReady();
}
