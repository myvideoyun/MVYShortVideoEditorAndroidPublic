package com.myvideoyun.shortvideo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageBeautyFilter;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageBrightnessFilter;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageLookupFilter;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageSaturationFilter;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageStickerFilter;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageZoomFilter;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.inputOutput.MVYGPUImageTextureInput;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.inputOutput.MVYGPUImageTextureOutput;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFilter;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES20.*;

/**
 * Created by 汪洋 on 2018/12/10.
 * Copyright © 2018年 myvideoyun. All rights reserved.
 */
public class MVYCameraEffectHandler {

    private MVYGPUImageTextureInput textureInput;
    private MVYGPUImageTextureOutput textureOutput;

    private MVYGPUImageFilter commonInputFilter;
    private MVYGPUImageFilter commonOutputFilter;

    private MVYGPUImageLookupFilter lookupFilter;
    private MVYGPUImageBeautyFilter beautyFilter;
    private MVYGPUImageBrightnessFilter brightnessFilter;
    private MVYGPUImageSaturationFilter saturationFilter;
    private MVYGPUImageZoomFilter zoomFilter;
    private MVYGPUImageStickerFilter stickerFilter;

    private boolean initCommonProcess = false;
    private boolean initProcess = false;

    private int[] bindingFrameBuffer = new int[1];
    private int[] bindingRenderBuffer = new int[1];
    private int[] viewPoint = new int[4];
    private int vertexAttribEnableArraySize = 5;
    private ArrayList<Integer> vertexAttribEnableArray = new ArrayList(vertexAttribEnableArraySize);

    public MVYCameraEffectHandler(final Context context) {

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable(){
            @Override
            public void run() {
                textureInput = new MVYGPUImageTextureInput();
                textureOutput = new MVYGPUImageTextureOutput();

                commonInputFilter = new MVYGPUImageFilter();
                commonOutputFilter = new MVYGPUImageFilter();

                try {
                    Bitmap lookupBitmap = BitmapFactory.decodeStream(context.getAssets().open("lookup.png"));
                    lookupFilter = new MVYGPUImageLookupFilter(lookupBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                beautyFilter = new MVYGPUImageBeautyFilter();
                zoomFilter = new MVYGPUImageZoomFilter();
                stickerFilter = new MVYGPUImageStickerFilter();
                brightnessFilter = new MVYGPUImageBrightnessFilter();
                saturationFilter = new MVYGPUImageSaturationFilter();
            }
        });
    }

    public void setStyle(Bitmap lookup) {
        if (lookupFilter != null) {
            lookupFilter.setLookup(lookup);
        }
    }

    public void setIntensityOfStyle(float intensity) {
        if (lookupFilter != null) {
            lookupFilter.setIntensity(intensity);
        }
    }

    public void setIntensityOfBeauty(float intensity) {
        if (beautyFilter != null) {
            beautyFilter.setIntensity(intensity);
        }
    }

    public void setIntensityOfBrightness(float intensity) {
        if (brightnessFilter != null) {
            brightnessFilter.setBrightnessIntensity(intensity);
        }
    }

    public void setIntensityOfSaturation(float intensity) {
        if (saturationFilter != null) {
            saturationFilter.setSaturationIntensity(intensity);
        }
    }

    public void setZoom(float zoom) {
        if (zoomFilter != null) {
            zoomFilter.setZoom(zoom);
        }
    }

    public float getZoom(){
        if (zoomFilter != null){
            return zoomFilter.getZoom();
        }
        return 0;
    }

    public void addSticker(MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerModel) {
        if (stickerFilter != null) {
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


    public void setRotateMode(MVYGPUImageConstants.MVYGPUImageRotationMode rotateMode) {
        this.textureInput.setRotateMode(rotateMode);

        if (rotateMode == MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateLeft) {
            rotateMode = MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateRight;
        }else if (rotateMode == MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateRight) {
            rotateMode = MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateLeft;
        }

        this.textureOutput.setRotateMode(rotateMode);
    }

    public void commonProcess() {

        List<MVYGPUImageFilter> filterChainArray = new ArrayList<MVYGPUImageFilter>();

        if (lookupFilter != null) {
            filterChainArray.add(lookupFilter);
        }
        if (beautyFilter != null) {
            filterChainArray.add(beautyFilter);
        }
        if (brightnessFilter != null) {
            filterChainArray.add(brightnessFilter);
        }
        if(saturationFilter != null) {
            filterChainArray.add(saturationFilter);
        }
        if (zoomFilter != null) {
            filterChainArray.add(zoomFilter);
        }
        if (stickerFilter != null) {
            filterChainArray.add(stickerFilter);
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

    public void processWithTexture(final int texture, final int width, final int height) {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {

                saveOpenGLState();

                commonProcess();

                if (!initProcess) {
                    textureInput.addTarget(commonInputFilter);
                    commonOutputFilter.addTarget(textureOutput);
                    initProcess = true;
                }

                // 设置输出的Filter
                textureOutput.setOutputWithBGRATexture(texture, width, height);

                // 设置输入的Filter, 同时开始处理纹理数据
                textureInput.processWithBGRATexture(texture, width, height);

                restoreOpenGLState();
            }
        });
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

    public void destroy() {
        textureInput.destroy();
        textureOutput.destroy();
        commonInputFilter.destroy();
        commonOutputFilter.destroy();

        if (lookupFilter != null) {
            lookupFilter.destroy();
        }
        if (beautyFilter != null) {
            beautyFilter.destroy();
        }
        if (brightnessFilter != null) {
            brightnessFilter.destroy();
        }
        if (saturationFilter != null) {
            saturationFilter.destroy();
        }
        if (zoomFilter != null) {
            zoomFilter.destroy();
        }
        if (stickerFilter != null) {
            stickerFilter.destroy();
        }
    }
}
