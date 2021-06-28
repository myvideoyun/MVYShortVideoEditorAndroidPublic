package com.myvideoyun.shortvideo.GPUImageCustomFilter;

import android.os.SystemClock;
import android.util.Log;

import com.myvideoyun.shortvideo.GPUImage.MVYGLProgram;
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
public class MVYGPUImageSaturationFilter extends MVYGPUImageFilter {

    public static final String kMVYGPUImageColorMatrixFragmentShaderString = "" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "\n" +
            "uniform lowp float saturation;\n" +
            "\n" +
            "const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "    lowp float luminance = dot(textureColor.rgb, luminanceWeighting);\n" +
            "    lowp vec3 greyScaleColor = vec3(luminance);\n" +
            "\n" +
            "gl_FragColor = vec4(mix(greyScaleColor, textureColor.rgb, saturation), textureColor.w);\n" +
            "}\n";

    private float[] colorMatrix = {
        1.f, 0.f, 0.f, 0.f,
        0.f, 1.f, 0.f, 0.f,
        0.f, 0.f, 1.f, 0.f,
        0.f, 0.f, 0.f, 1.f};

    private int saturationUniform;

    private float saturationIntensity = 1.0f;

    public MVYGPUImageSaturationFilter() {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram = new MVYGLProgram(kMVYGPUImageVertexShaderString, kMVYGPUImageColorMatrixFragmentShaderString);
                filterProgram.link();

                filterPositionAttribute = filterProgram.attributeIndex("position");
                filterTextureCoordinateAttribute = filterProgram.attributeIndex("inputTextureCoordinate");
                filterInputTextureUniform = filterProgram.uniformIndex("inputImageTexture");
                saturationUniform = filterProgram.uniformIndex("saturation");
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

                glUniform1f(saturationUniform, saturationIntensity);

                glEnableVertexAttribArray(filterPositionAttribute);
                glEnableVertexAttribArray(filterTextureCoordinateAttribute);

                glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, vertices);
                glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, textureCoordinates);

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                glDisableVertexAttribArray(filterPositionAttribute);
                glDisableVertexAttribArray(filterTextureCoordinateAttribute);
            }
        });

        Log.d(TAG, "saturationFilter process time: " + (SystemClock.elapsedRealtime() - time));
    }

    public void setSaturationIntensity(float intensity) {
        saturationIntensity = intensity;
    }
}
