package com.myvideoyun.shortvideo;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFilter;
import com.myvideoyun.shortvideo.GPUImage.MVYGLProgram;

import java.nio.Buffer;

import static android.opengl.GLES20.*;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFit;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.getAspectRatioInsideSize;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext.syncRunOnRenderThread;

/**
 * Created by 汪洋 on 2019/2/11.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class MVYPreviewView extends SurfaceView implements SurfaceHolder.Callback {

    private MVYGPUImageEGLContext eglContext;

    private int boundingWidth;
    private int boundingHeight;

    private MVYGLProgram filterProgram;

    private int filterPositionAttribute, filterTextureCoordinateAttribute;
    private int filterInputTextureUniform;

    private Buffer textureCoordinates = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.noRotationTextureCoordinates);

    private MVYGPUImageConstants.MVYGPUImageContentMode contentMode = MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFit;

    public MVYPreviewView(Context context) {
        super(context);
        commonInit();
    }

    public MVYPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        commonInit();
    }

    public MVYPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        commonInit();
    }

    private void commonInit() {
        getHolder().addCallback(this);
    }

    /**
     * 设置窗口缩放方式
     */
    public void setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode contentMode) {
        this.contentMode = contentMode;
    }

    /**
     * 渲染纹理图像到surface上
     */
    public void render(final int texture, final int width, final int height) {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                if (eglContext == null) {
                    return;
                }

                eglContext.makeCurrent();

                filterProgram.use();

                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                glViewport(0, 0, boundingWidth, boundingHeight);

                glClearColor(0, 0, 0, 0);
                glClear(GL_COLOR_BUFFER_BIT);

                glActiveTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_2D, texture);

                glUniform1i(filterInputTextureUniform, 2);

                PointF insetSize = MVYGPUImageConstants.getAspectRatioInsideSize(new PointF(width, height), new PointF(boundingWidth, boundingHeight));

                float widthScaling = 0.0f, heightScaling = 0.0f;

                switch (contentMode) {
                    case kMVYGPUImageScaleToFill:
                        widthScaling = 1.0f;
                        heightScaling = 1.0f;
                        break;
                    case kMVYGPUImageScaleAspectFit:
                        widthScaling = insetSize.x / boundingWidth;
                        heightScaling = insetSize.y / boundingHeight;
                        break;
                    case kMVYGPUImageScaleAspectFill:
                        widthScaling = boundingHeight / insetSize.y;
                        heightScaling = boundingWidth / insetSize.x;
                        break;
                }

                float squareVertices[] = new float[8];
                squareVertices[0] = -widthScaling;
                squareVertices[1] = -heightScaling;
                squareVertices[2] = widthScaling;
                squareVertices[3] = -heightScaling;
                squareVertices[4] = -widthScaling;
                squareVertices[5] = heightScaling;
                squareVertices[6] = widthScaling;
                squareVertices[7] = heightScaling;

                glEnableVertexAttribArray(filterPositionAttribute);
                glEnableVertexAttribArray(filterTextureCoordinateAttribute);

                Buffer imageVertices = MVYGPUImageConstants.floatArrayToBuffer(squareVertices);

                glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, imageVertices);
                glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, textureCoordinates);

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                glDisableVertexAttribArray(filterPositionAttribute);
                glDisableVertexAttribArray(filterTextureCoordinateAttribute);

                eglContext.swapBuffers();
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        createEGLContext(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.boundingWidth = width;
        this.boundingHeight = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        destroyEGLContext();
    }

    /**
     * 创建EGL 和 GLES 环境
     * @param object SurfaceView、SurfaceTexture、SurfaceHolder 或 Surface
     */
    private void createEGLContext(final Object object) {

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                eglContext = new MVYGPUImageEGLContext();
                eglContext.initEGLWindow(object);

                filterProgram = new MVYGLProgram(MVYGPUImageFilter.kMVYGPUImageVertexShaderString, MVYGPUImageFilter.kMVYGPUImagePassthroughFragmentShaderString);
                filterProgram.link();

                filterPositionAttribute = filterProgram.attributeIndex("position");
                filterTextureCoordinateAttribute = filterProgram.attributeIndex("inputTextureCoordinate");
                filterInputTextureUniform = filterProgram.uniformIndex("inputImageTexture");
                filterProgram.use();
            }
        });
    }

    /**
     * 销毁EGL 和 GLES 环境
     */
    private void destroyEGLContext() {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram.destroy();

                eglContext.destroyEGLWindow();
                eglContext = null;
            }
        });
    }
}
