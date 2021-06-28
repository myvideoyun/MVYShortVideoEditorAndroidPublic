package com.myvideoyun.shortvideo.GPUImage;

import java.nio.Buffer;

import static android.opengl.GLES20.*;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext.syncRunOnRenderThread;

/**
 * Created by 汪洋 on 2018/12/8.
 * Copyright © 2018年 myvideoyun. All rights reserved.
 */
public class MVYGPUImageFilter extends MVYGPUImageOutput implements MVYGPUImageInput{

    public static final String kMVYGPUImageVertexShaderString = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";

    public static final String kMVYGPUImagePassthroughFragmentShaderString = "" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    private Buffer imageVertices = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.imageVertices);
    private Buffer textureCoordinates = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.noRotationTextureCoordinates);

    protected MVYGPUImageFramebuffer outputFramebuffer;
    protected MVYGPUImageFramebuffer firstInputFramebuffer;

    protected MVYGLProgram filterProgram;

    protected int filterPositionAttribute, filterTextureCoordinateAttribute;
    protected int filterInputTextureUniform;

    protected int inputWidth;
    protected int inputHeight;

    public MVYGPUImageFilter(final String vertexShaderString, final String fragmentShaderString) {
        syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram = new MVYGLProgram(vertexShaderString, fragmentShaderString);
                filterProgram.link();

                filterPositionAttribute = filterProgram.attributeIndex("position");
                filterTextureCoordinateAttribute = filterProgram.attributeIndex("inputTextureCoordinate");
                filterInputTextureUniform = filterProgram.uniformIndex("inputImageTexture");
                filterProgram.use();
            }
        });
    }

    public MVYGPUImageFilter() {
        this(kMVYGPUImageVertexShaderString, kMVYGPUImagePassthroughFragmentShaderString);
    }

    protected void renderToTexture(final Buffer vertices, final Buffer textureCoordinates) {
        syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
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
                glBindTexture(GL_TEXTURE_2D, firstInputFramebuffer.texture[0]);

                glUniform1i(filterInputTextureUniform, 2);

                glEnableVertexAttribArray(filterPositionAttribute);
                glEnableVertexAttribArray(filterTextureCoordinateAttribute);

                glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, vertices);
                glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, textureCoordinates);

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                glDisableVertexAttribArray(filterPositionAttribute);
                glDisableVertexAttribArray(filterTextureCoordinateAttribute);
            }
        });
    }

    protected void informTargetsAboutNewFrame() {
        for (MVYGPUImageInput currentTarget : getTargets()) {
            currentTarget.setInputSize(outputWidth(), outputHeight());
            currentTarget.setInputFramebuffer(outputFramebuffer);
        }

        for (MVYGPUImageInput currentTarget : getTargets()) {
            currentTarget.newFrameReady();
        }
    }

    protected int outputWidth() {
        return inputWidth;
    }

    protected int outputHeight() {
        return inputHeight;
    }

    @Override
    public void setInputSize(int width, int height) {
        inputWidth = width;
        inputHeight = height;
    }

    @Override
    public void setInputFramebuffer(MVYGPUImageFramebuffer newInputFramebuffer) {
        firstInputFramebuffer = newInputFramebuffer;
    }

    @Override
    public void newFrameReady() {
        renderToTexture(imageVertices, textureCoordinates);
        informTargetsAboutNewFrame();
    }

    public void destroy() {
        removeAllTargets();

        syncRunOnRenderThread(new Runnable() {
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