package com.myvideoyun.shortvideo.GPUImageCustomFilter;

import android.graphics.Bitmap;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFilter;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE2;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameterf;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.TAG;

/**
 * Created by 汪洋 on 2018/12/10.
 * Copyright © 2018年 myvideoyun. All rights reserved.
 */
public class MVYGPUImageStickerFilter extends MVYGPUImageFilter {

    public static final String kMVYGPUImageStickerVertexShaderString = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "uniform mat4 transformMatrix;\n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = transformMatrix * vec4(position.xyz, 1.0);\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";

    private List<MVYGPUImageStickerModel> stickers = new ArrayList<>();

    private int transformMatrixUniform;

    public MVYGPUImageStickerFilter() {
        super(kMVYGPUImageStickerVertexShaderString, kMVYGPUImagePassthroughFragmentShaderString);

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                transformMatrixUniform = filterProgram.uniformIndex("transformMatrix");
            }
        });
    }

    protected void renderToTexture(final Buffer vertices, final Buffer textureCoordinates) {
        long time = SystemClock.elapsedRealtime();

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram.use();

                float[] identityTransformMatrix = new float[16];
                Matrix.setIdentityM(identityTransformMatrix, 0);
                glUniformMatrix4fv(transformMatrixUniform, 1, false, identityTransformMatrix, 0);
            }
        });

        super.renderToTexture(vertices, textureCoordinates);

        // 循环渲染纹理贴图
        for (final MVYGPUImageStickerModel model : stickers) {
            MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    filterProgram.use();

                    glEnable(GL_BLEND);
                    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

                    glActiveTexture(GL_TEXTURE2);
                    glBindTexture(GL_TEXTURE_2D, model.texture[0]);

                    glUniform1i(filterInputTextureUniform, 2);

                    float[] transformMatrix = model.transformMatrix.clone();
                    Matrix.rotateM(transformMatrix, 0, 180.f, 1.f, 0.f, 0.f);
                    Matrix.scaleM(transformMatrix, 0, (float)model.width / inputWidth,(float)model.height / inputHeight,1.0f);
                    glUniformMatrix4fv(transformMatrixUniform, 1, false, transformMatrix, 0);

                    glEnableVertexAttribArray(filterPositionAttribute);
                    glEnableVertexAttribArray(filterTextureCoordinateAttribute);

                    glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, vertices);
                    glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, textureCoordinates);

                    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                    glDisableVertexAttribArray(filterPositionAttribute);
                    glDisableVertexAttribArray(filterTextureCoordinateAttribute);

                    glDisable(GL_BLEND);
                }
            });
        }

        Log.d(TAG, "stickerFilter process time: " + (SystemClock.elapsedRealtime() - time));
    }

    public void addSticker(final MVYGPUImageStickerModel stickerModel) {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                stickers.add(stickerModel);
            }
        });
    }

    public void removeSticker(final MVYGPUImageStickerModel stickerModel) {
        if (stickerModel == null) return;
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                stickers.remove(stickerModel);
                stickerModel.destroy();
            }
        });
    }

    public void clear() {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                stickers.clear();
            }
        });
    }

    @Override
    public void destroy() {
        super.destroy();
        clear();
    }

    public static class MVYGPUImageStickerModel {
        public int[] texture = new int[1];
        public int width;
        public int height;
        public float[] transformMatrix = new float[16];

        /**
         * 创建一个纹理贴图对象
         */
        public MVYGPUImageStickerModel(final Bitmap bitmap){
            MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    int size = bitmap.getRowBytes() * bitmap.getHeight();
                    ByteBuffer pixelBuffer = ByteBuffer.allocate(size);
                    pixelBuffer.order(ByteOrder.BIG_ENDIAN);
                    bitmap.copyPixelsToBuffer(pixelBuffer);
                    pixelBuffer.position(0);

                    glGenTextures(1, texture, 0);
                    glBindTexture(GL_TEXTURE_2D, texture[0]);
                    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

                    glBindTexture(GL_TEXTURE_2D, texture[0]);
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, bitmap.getWidth(), bitmap.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);

                    bitmap.recycle();
                }
            });

            Matrix.setIdentityM(transformMatrix, 0);

            width = bitmap.getWidth();
            height = bitmap.getHeight();
        }

        /**
         * 销毁一个纹理贴图对象
         */
        public void destroy() {
            MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    if (texture[0] != 0) {
                        glDeleteTextures(1, texture, 0);
                        texture[0] = 0;
                    }
                }
            });
        }
    }
}
