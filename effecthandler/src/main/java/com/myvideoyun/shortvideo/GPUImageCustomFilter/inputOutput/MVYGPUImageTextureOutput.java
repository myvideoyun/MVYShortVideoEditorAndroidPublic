package com.myvideoyun.shortvideo.GPUImageCustomFilter.inputOutput;

import android.util.Log;

import com.myvideoyun.shortvideo.GPUImage.MVYGLProgram;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFramebuffer;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageInput;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFilter;

import java.nio.Buffer;

import static android.opengl.GLES20.*;

/**
 * Created by 汪洋 on 2018/12/10.
 * Copyright © 2018年 myvideoyun. All rights reserved.
 */
public class MVYGPUImageTextureOutput implements MVYGPUImageInput {

    private Buffer imageVertices = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.imageVertices);

    protected MVYGPUImageFramebuffer firstInputFramebuffer;

    protected MVYGLProgram filterProgram;

    protected int filterPositionAttribute, filterTextureCoordinateAttribute;
    protected int filterInputTextureUniform;

    protected int inputWidth;
    protected int inputHeight;

    private MVYGPUImageConstants.MVYGPUImageRotationMode rotateMode = MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageNoRotation;

    private int[] framebuffer = new int[1];
    public int[] texture = new int[1];

    public MVYGPUImageTextureOutput() {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram = new MVYGLProgram(MVYGPUImageFilter.kMVYGPUImageVertexShaderString, MVYGPUImageFilter.kMVYGPUImagePassthroughFragmentShaderString);
                filterProgram.link();

                filterPositionAttribute = filterProgram.attributeIndex("position");
                filterTextureCoordinateAttribute = filterProgram.attributeIndex("inputTextureCoordinate");
                filterInputTextureUniform = filterProgram.uniformIndex("inputImageTexture");
                filterProgram.use();
            }
        });
    }

    protected void renderToTexture(final Buffer vertices, final Buffer textureCoordinates) {

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram.use();

                glBindFramebuffer(GL_FRAMEBUFFER, framebuffer[0]);
                glViewport(0, 0, inputWidth, inputHeight);

                glClearColor(0, 0, 0, 0);
                glClear(GL_COLOR_BUFFER_BIT);

                glActiveTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_2D, firstInputFramebuffer.texture[0]);

                glUniform1i(filterInputTextureUniform, 2);

                glEnableVertexAttribArray(filterPositionAttribute);
                glEnableVertexAttribArray(filterTextureCoordinateAttribute);

                glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, vertices);
                glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, textureCoordinates);

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                glClearColor(1, 0, 0, 0);

                glDisableVertexAttribArray(filterPositionAttribute);
                glDisableVertexAttribArray(filterTextureCoordinateAttribute);
            }
        });
    }

    public void setOutputWithBGRATexture(final int textureId, int width, int height) {
        this.texture = new int[]{textureId};
        this.inputWidth = width;
        this.inputHeight = height;

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable(){
            @Override
            public void run() {
                if (framebuffer[0] == 0){
                    glGenFramebuffers(1, framebuffer, 0);
                    Log.d(MVYGPUImageConstants.TAG, "创建一个 OpenGL frameBuffer " + framebuffer[0]);
                }
                glBindFramebuffer(GL_FRAMEBUFFER, framebuffer[0]);

                glBindTexture(GL_TEXTURE_2D, texture[0]);

                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, inputWidth, inputHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture[0], 0);

                glBindTexture(GL_TEXTURE_2D, 0);
            }
        });
    }

    public void setRotateMode(MVYGPUImageConstants.MVYGPUImageRotationMode rotateMode) {
        this.rotateMode = rotateMode;
    }

    public void destroy() {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram.destroy();

                if (framebuffer[0] != 0){
                    Log.d(MVYGPUImageConstants.TAG, "销毁一个 OpenGL frameBuffer " + framebuffer[0]);
                    glDeleteFramebuffers(1, framebuffer, 0);
                    framebuffer[0] = 0;
                }
            }
        });
    }

    @Override
    public void setInputSize(int width, int height) {

    }

    @Override
    public void setInputFramebuffer(MVYGPUImageFramebuffer newInputFramebuffer) {
        firstInputFramebuffer = newInputFramebuffer;
    }

    @Override
    public void newFrameReady() {
        renderToTexture(imageVertices,  MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.textureCoordinatesForRotation(rotateMode)));
    }
}
