package com.myvideoyun.shortvideo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageShortVideoFilter;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageStickerFilter;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.inputOutput.MVYGPUImageTextureOutput;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.inputOutput.MVYGPUImageYUVDataInput;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFilter;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_FRAMEBUFFER_BINDING;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_RENDERBUFFER;
import static android.opengl.GLES20.GL_RENDERBUFFER_BINDING;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.GL_VERTEX_ATTRIB_ARRAY_ENABLED;
import static android.opengl.GLES20.GL_VIEWPORT;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindRenderbuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glGetVertexAttribiv;
import static android.opengl.GLES20.glReadPixels;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glViewport;

/**
 * Created by 汪洋 on 2018/12/10.
 * Copyright © 2018年 myvideoyun. All rights reserved.
 */
public class MVYVideoEffectHandler {

    private int[] outputTexture = {0};

    private MVYGPUImageYUVDataInput yuvDataInput;
    private MVYGPUImageTextureOutput textureOutput;

    private MVYGPUImageFilter commonInputFilter;
    private MVYGPUImageFilter commonOutputFilter;

    private MVYGPUImageStickerFilter stickerFilter;
    private MVYGPUImageShortVideoFilter shortVideoFilter;

    private boolean initCommonProcess = false;
    private boolean initProcess = false;

    private int[] bindingFrameBuffer = new int[1];
    private int[] bindingRenderBuffer = new int[1];
    private int[] viewPoint = new int[4];
    private int vertexAttribEnableArraySize = 5;
    private ArrayList<Integer> vertexAttribEnableArray = new ArrayList(vertexAttribEnableArraySize);

    public int outputWidth;
    public int outputHeight;

    public MVYVideoEffectHandler(final Context context) {

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable(){
            @Override
            public void run() {

                if (outputTexture[0] == 0) {
                    glGenTextures(1, outputTexture, 0);
                    glBindTexture(GL_TEXTURE_2D, outputTexture[0]);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                    glBindTexture(GL_TEXTURE_2D, 0);
                }

                yuvDataInput = new MVYGPUImageYUVDataInput();
                textureOutput = new MVYGPUImageTextureOutput();

                commonInputFilter = new MVYGPUImageFilter();
                commonOutputFilter = new MVYGPUImageFilter();

                stickerFilter = new MVYGPUImageStickerFilter();
                shortVideoFilter = new MVYGPUImageShortVideoFilter();
                setTypeOfShortVideo(MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_NONE);
            }
        });
    }

    private void commonProcess() {

        List<MVYGPUImageFilter> filterChainArray = new ArrayList<MVYGPUImageFilter>();

        if (stickerFilter != null) {
            filterChainArray.add(stickerFilter);
        }

        if (shortVideoFilter != null) {
            filterChainArray.add(shortVideoFilter);
        }

        if (!initCommonProcess) {

            if (filterChainArray.size() > 0) {
                commonInputFilter.addTarget(filterChainArray.get(0));
                for (int x = 0; x < filterChainArray.size() - 1; x++) {
                    filterChainArray.get(x).addTarget(filterChainArray.get(x+1));
                }
                filterChainArray.get(filterChainArray.size()-1).addTarget(commonOutputFilter);

            }else {
                commonInputFilter.addTarget(commonOutputFilter);
            }

            initCommonProcess = true;
        }
    }

    public int processWithYUVData(final byte[] yData, final byte[] uData, final byte[] vData, final int width, final int height, final int lineSize, final MVYGPUImageConstants.MVYGPUImageRotationMode rotateMode) {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {

                saveOpenGLState();

                commonProcess();

                if (!initProcess) {
                    yuvDataInput.addTarget(commonInputFilter);
                    commonOutputFilter.addTarget(textureOutput);
                    initProcess = true;
                }

                // 设置输出的Filter
                yuvDataInput.setRotateMode(rotateMode);

                if (MVYGPUImageConstants.needExchangeWidthAndHeightWithRotation(rotateMode)) {

                    outputWidth = height;
                    outputHeight = width;

                    textureOutput.setOutputWithBGRATexture(outputTexture[0], height, width);
                } else {

                    outputWidth = width;
                    outputHeight = height;

                    textureOutput.setOutputWithBGRATexture(outputTexture[0], width, height);
                }

                // 设置输入的Filter, 同时开始处理纹理数据
                yuvDataInput.processWithYUV(yData, uData, vData, width, height, lineSize);

                restoreOpenGLState();
            }
        });

        return outputTexture[0];
    }

    private void saveOpenGLState() {
        // 获取当前绑定的FrameBuffer
        glGetIntegerv(GL_FRAMEBUFFER_BINDING, bindingFrameBuffer, 0);

        // 获取当前绑定的RenderBuffer
        glGetIntegerv(GL_RENDERBUFFER_BINDING, bindingRenderBuffer, 0);

        // 获取viewpoint
        glGetIntegerv(GL_VIEWPORT, viewPoint, 0);

        // 获取顶点数据
        vertexAttribEnableArray.clear();
        for (int x = 0 ; x < vertexAttribEnableArraySize; x++) {
            int[] vertexAttribEnable = new int[1];
            glGetVertexAttribiv(x, GL_VERTEX_ATTRIB_ARRAY_ENABLED, vertexAttribEnable, 0);
            if (vertexAttribEnable[0] != 0) {
                vertexAttribEnableArray.add(x);
            }
        }
    }

    private void restoreOpenGLState() {
        // 还原当前绑定的FrameBuffer
        glBindFramebuffer(GL_FRAMEBUFFER, bindingFrameBuffer[0]);

        // 还原当前绑定的RenderBuffer
        glBindRenderbuffer(GL_RENDERBUFFER, bindingRenderBuffer[0]);

        // 还原viewpoint
        glViewport(viewPoint[0], viewPoint[1], viewPoint[2], viewPoint[3]);

        // 还原顶点数据
        for (int x = 0 ; x < vertexAttribEnableArray.size(); x++) {
            glEnableVertexAttribArray(vertexAttribEnableArray.get(x));
        }
    }

    public void addSticker(MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerModel) {
        if (stickerFilter != null && stickerModel != null) {
            stickerFilter.addSticker(stickerModel);
        }
    }

    public void removeSticker(MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerModel) {
        if (stickerFilter != null) {
            stickerFilter.removeSticker(stickerModel);
        }
    }

    public void clearSticker() {
        if (stickerFilter != null) {
            stickerFilter.clear();
        }
    }

    public void setTypeOfShortVideo(MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE type) {
        if (shortVideoFilter != null) {
            shortVideoFilter.setType(type);
        }
    }

    public Bitmap getCurrentImage(final int width, final int height) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(width*height*4);

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                glReadPixels(0,0,width,height,GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer);
            }
        });
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(byteBuffer);
        Matrix matrix = new Matrix();
        matrix.setScale(1, -1);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

        return bitmap;
    }

    public void destroy() {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                if (outputTexture[0] != 0) {
                    glDeleteTextures(1, outputTexture, 0);
                    outputTexture[0] = 0;
                }
            }
        });

        yuvDataInput.destroy();
        textureOutput.destroy();
        commonInputFilter.destroy();
        commonOutputFilter.destroy();

        if (stickerFilter != null) {
            stickerFilter.destroy();
            stickerFilter = null;
        }

        if (shortVideoFilter != null) {
            shortVideoFilter.destroy();
            shortVideoFilter = null;
        }

    }

}
