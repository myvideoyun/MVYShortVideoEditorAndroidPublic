package com.myvideoyun.shortvideo.GPUImageCustomFilter.inputOutput;

import com.myvideoyun.shortvideo.GPUImage.MVYGLProgram;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFramebuffer;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageInput;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageOutput;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFilter;

import java.nio.Buffer;

import static android.opengl.GLES20.*;

/**
 * Created by 汪洋 on 2018/12/10.
 * Copyright © 2018年 myvideoyun. All rights reserved.
 */
public class MVYGPUImageTextureInput extends MVYGPUImageOutput {

    private Buffer imageVertices = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.imageVertices);

    protected MVYGPUImageFramebuffer outputFramebuffer;

    protected MVYGLProgram filterProgram;

    protected int filterPositionAttribute, filterTextureCoordinateAttribute;
    protected int filterInputTextureUniform;

    private MVYGPUImageConstants.MVYGPUImageRotationMode rotateMode = MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageNoRotation;

    public MVYGPUImageTextureInput() {
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

    public void processWithBGRATexture(final int texture, final int width, final int height) {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {

                int inputWidth = width;
                int inputHeight = height;

                if (MVYGPUImageConstants.needExchangeWidthAndHeightWithRotation(rotateMode)) {
                    int temp = width;
                    inputWidth = height;
                    inputHeight = temp;
                }

                filterProgram.use();

                if (outputFramebuffer != null) {
                    if (inputWidth != outputFramebuffer.width || inputHeight != outputFramebuffer.height) {
                        outputFramebuffer.destroy();
                        outputFramebuffer = null;
                    }
                }

                if (outputFramebuffer == null) {
                    outputFramebuffer = new MVYGPUImageFramebuffer(inputWidth, inputHeight);
                }

                outputFramebuffer.activateFramebuffer();

                glClearColor(0, 0, 0, 0);
                glClear(GL_COLOR_BUFFER_BIT);

                glActiveTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_2D, texture);

                glUniform1i(filterInputTextureUniform, 2);

                glEnableVertexAttribArray(filterPositionAttribute);
                glEnableVertexAttribArray(filterTextureCoordinateAttribute);

                glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, imageVertices);
                glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.textureCoordinatesForRotation(rotateMode)));

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                glDisableVertexAttribArray(filterPositionAttribute);
                glDisableVertexAttribArray(filterTextureCoordinateAttribute);

                for (MVYGPUImageInput currentTarget : getTargets()) {
                    currentTarget.setInputSize(inputWidth, inputHeight);
                    currentTarget.setInputFramebuffer(outputFramebuffer);
                }

                for (MVYGPUImageInput currentTarget : getTargets()) {
                    currentTarget.newFrameReady();
                }
            }
        });
    }

    public void setRotateMode(MVYGPUImageConstants.MVYGPUImageRotationMode rotateMode) {
        this.rotateMode = rotateMode;
    }

    public void destroy() {
        removeAllTargets();

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram.destroy();

                if (outputFramebuffer != null) {
                    outputFramebuffer.destroy();
                }
            }
        });
    }
}
