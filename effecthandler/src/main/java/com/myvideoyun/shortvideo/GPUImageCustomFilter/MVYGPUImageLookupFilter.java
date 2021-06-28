package com.myvideoyun.shortvideo.GPUImageCustomFilter;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import com.myvideoyun.shortvideo.GPUImage.MVYGLProgram;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFilter;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFramebuffer;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.opengl.GLES20.*;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.TAG;

/**
 * Created by 汪洋 on 2018/12/8.
 * Copyright © 2018年 myvideoyun. All rights reserved.
 */
public class MVYGPUImageLookupFilter extends MVYGPUImageFilter {

    public static final String kMVYGPUImageLookupFragmentShaderString = "" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform sampler2D inputImageTexture2;\n" +
            "\n" +
            "uniform lowp float intensity;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "\n" +
            "    highp float blueColor = textureColor.b * 63.0;\n" +
            "\n" +
            "    highp vec2 quad1;\n" +
            "    quad1.y = floor(floor(blueColor) / 8.0);\n" +
            "    quad1.x = floor(blueColor) - (quad1.y * 8.0);\n" +
            "\n" +
            "    highp vec2 quad2;\n" +
            "    quad2.y = floor(ceil(blueColor) / 8.0);\n" +
            "    quad2.x = ceil(blueColor) - (quad2.y * 8.0);\n" +
            "\n" +
            "    highp vec2 texPos1;\n" +
            "    texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);\n" +
            "    texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n" +
            "\n" +
            "    highp vec2 texPos2;\n" +
            "    texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);\n" +
            "    texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n" +
            "\n" +
            "    lowp vec4 newColor1 = texture2D(inputImageTexture2, texPos1);\n" +
            "    lowp vec4 newColor2 = texture2D(inputImageTexture2, texPos2);\n" +
            "\n" +
            "    lowp vec4 newColor = mix(newColor1, newColor2, fract(blueColor));\n" +
            "    gl_FragColor = mix(textureColor, vec4(newColor.rgb, textureColor.w), intensity);\n" +
            "}";

    private Bitmap lookup;
    private float intensity = 0.8f;

    private int filterInputTextureUniform2;
    private int intensityUniform;

    private int lookupTexture[] = new int[1];
    private boolean updateLookupTexture = true;

    public MVYGPUImageLookupFilter(final Bitmap lookup) {
        this.lookup = lookup;
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram = new MVYGLProgram(kMVYGPUImageVertexShaderString, kMVYGPUImageLookupFragmentShaderString);
                filterProgram.link();

                filterPositionAttribute = filterProgram.attributeIndex("position");
                filterTextureCoordinateAttribute = filterProgram.attributeIndex("inputTextureCoordinate");
                filterInputTextureUniform = filterProgram.uniformIndex("inputImageTexture");
                filterInputTextureUniform2 = filterProgram.uniformIndex("inputImageTexture2");
                intensityUniform = filterProgram.uniformIndex("intensity");
                filterProgram.use();

                glGenTextures(1, lookupTexture, 0);
                glBindTexture(GL_TEXTURE_2D, lookupTexture[0]);
                glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            }
        });
    }

    public void setLookup(Bitmap lookup) {
        this.lookup = lookup;

        updateLookupTexture = true;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
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

                glActiveTexture(GL_TEXTURE3);
                if (updateLookupTexture) { // 更新Lookup

                    int size = lookup.getRowBytes() * lookup.getHeight();
                    ByteBuffer pixelBuffer = ByteBuffer.allocate(size);
                    pixelBuffer.order(ByteOrder.BIG_ENDIAN);
                    lookup.copyPixelsToBuffer(pixelBuffer);
                    pixelBuffer.position(0);

                    glBindTexture(GL_TEXTURE_2D, lookupTexture[0]);
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, lookup.getWidth(), lookup.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);

                    lookup.recycle();

                    updateLookupTexture = false;
                } else {
                    glBindTexture(GL_TEXTURE_2D, lookupTexture[0]);
                }

                glUniform1i(filterInputTextureUniform2, 3);

                glUniform1f(intensityUniform, intensity);

                glEnableVertexAttribArray(filterPositionAttribute);
                glEnableVertexAttribArray(filterTextureCoordinateAttribute);

                glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, vertices);
                glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, textureCoordinates);

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                glDisableVertexAttribArray(filterPositionAttribute);
                glDisableVertexAttribArray(filterTextureCoordinateAttribute);
            }
        });

        Log.d(TAG, "lookUpFilter process time: " + (SystemClock.elapsedRealtime() - time));
    }

    @Override
    public void destroy() {
        super.destroy();

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                if (lookupTexture[0] != 0) {
                    glDeleteTextures(1, lookupTexture, 0);
                    lookupTexture[0] = 0;
                }
            }
        });
    }
}
