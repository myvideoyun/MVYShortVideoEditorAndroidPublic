package com.myvideoyun.shortvideo.GPUImageCustomFilter;

import android.os.SystemClock;
import android.util.Log;

import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFilter;

import java.nio.Buffer;

import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.TAG;

/**
 * Created by 汪洋 on 2018/12/10.
 * Copyright © 2018年 myvideoyun. All rights reserved.
 */
public class MVYGPUImageZoomFilter extends MVYGPUImageFilter {

    private float zoom = 1.0f;

    protected void renderToTexture(Buffer vertices, Buffer textureCoordinates) {
        long time = SystemClock.elapsedRealtime();

        float imageVertices[] = {
                -1.0f * zoom, -1.0f * zoom,
                 1.0f * zoom, -1.0f * zoom,
                -1.0f * zoom,  1.0f * zoom,
                 1.0f * zoom,  1.0f * zoom,
        };

        vertices = MVYGPUImageConstants.floatArrayToBuffer(imageVertices);

        super.renderToTexture(vertices, textureCoordinates);

        Log.d(TAG, "zoomFilter process time: " + (SystemClock.elapsedRealtime() - time));
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public float getZoom(){
        return this.zoom;
    }
}
