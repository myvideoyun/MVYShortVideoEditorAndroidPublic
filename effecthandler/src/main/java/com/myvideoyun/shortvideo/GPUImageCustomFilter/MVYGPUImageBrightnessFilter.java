package com.myvideoyun.shortvideo.GPUImageCustomFilter;

import android.os.SystemClock;
import android.util.Log;

import com.myvideoyun.shortvideo.GPUImage.MVYGLProgram;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFilter;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFramebuffer;

import java.nio.Buffer;

import static android.opengl.GLES20.*;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.TAG;

/**
 * Created by 汪洋 on 2019/4/18.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class MVYGPUImageBrightnessFilter extends MVYGPUImageFilter {

    public static final String kMVYGPUImageColorMatrixFragmentShaderString = "" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "\n" +
            "uniform lowp mat4 colorMatrix;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "  lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "  lowp vec4 outputColor = textureColor * colorMatrix;\n" +
            "  \n" +
            "  gl_FragColor = outputColor;\n" +
            "}";

    private float[] colorMatrix = {
        1.f, 0.f, 0.f, 0.f,
        0.f, 1.f, 0.f, 0.f,
        0.f, 0.f, 1.f, 0.f,
        0.f, 0.f, 0.f, 1.f};

    private int colorMatrixUniform;

    private float brightnessIntensity = 1.0f;

    public MVYGPUImageBrightnessFilter() {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram = new MVYGLProgram(kMVYGPUImageVertexShaderString, kMVYGPUImageColorMatrixFragmentShaderString);
                filterProgram.link();

                filterPositionAttribute = filterProgram.attributeIndex("position");
                filterTextureCoordinateAttribute = filterProgram.attributeIndex("inputTextureCoordinate");
                filterInputTextureUniform = filterProgram.uniformIndex("inputImageTexture");
                colorMatrixUniform = filterProgram.uniformIndex("colorMatrix");
                filterProgram.use();
            }
        });
    }

    @Override
    protected void renderToTexture(final Buffer vertices, final Buffer textureCoordinates) {
        long time = SystemClock.elapsedRealtime();

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
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

                glUniformMatrix4fv(colorMatrixUniform, 1, false, colorMatrix, 0);

                glEnableVertexAttribArray(filterPositionAttribute);
                glEnableVertexAttribArray(filterTextureCoordinateAttribute);

                glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, vertices);
                glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, textureCoordinates);

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                glDisableVertexAttribArray(filterPositionAttribute);
                glDisableVertexAttribArray(filterTextureCoordinateAttribute);
            }
        });

        Log.d(TAG, "brightnessFilter process time: " + (SystemClock.elapsedRealtime() - time));
    }

    public void setBrightnessIntensity(float intensity) {
        brightnessIntensity = intensity;

        processBrightnessAndSaturation();
    }

    private void processBrightnessAndSaturation() {
        // reset
        colorMatrix = new float[]{
                1.f, 0.f, 0.f, 0.f,
                0.f, 1.f, 0.f, 0.f,
                0.f, 0.f, 1.f, 0.f,
                0.f, 0.f, 0.f, 1.f};

        // 处理亮度
        float intensity = brightnessIntensity;
        float mmat[] = new float[16];

        int idx = 0;
        mmat[idx++] = intensity;
        mmat[idx++] = 0.0f;
        mmat[idx++] = 0.0f;
        mmat[idx++] = 0.0f;

        mmat[idx++] = 0.0f;
        mmat[idx++] = intensity;
        mmat[idx++] = 0.0f;
        mmat[idx++] = 0.0f;

        mmat[idx++] = 0.0f;
        mmat[idx++] = 0.0f;
        mmat[idx++] = intensity;
        mmat[idx++] = 0.0f;

        mmat[idx++] = 0.0f;
        mmat[idx++] = 0.0f;
        mmat[idx++] = 0.0f;
        mmat[idx] = 1.0f;

        colorMatrix = MVYGPUImageConstants.matrix4fvMult(mmat, colorMatrix);
    }
}
