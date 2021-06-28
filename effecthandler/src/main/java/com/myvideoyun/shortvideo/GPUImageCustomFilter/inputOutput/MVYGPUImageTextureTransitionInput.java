package com.myvideoyun.shortvideo.GPUImageCustomFilter.inputOutput;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.opengl.Matrix;

import com.myvideoyun.shortvideo.GPUImage.MVYGLProgram;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFilter;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFramebuffer;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageInput;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageOutput;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES20.*;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageNoRotation;

public class MVYGPUImageTextureTransitionInput extends MVYGPUImageOutput {
    public static final String kMVYGPUImageTransitionVertexShaderString = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "uniform mat4 transformMatrix;\n" +
            "uniform mat4 orthographicMatrix;\n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = transformMatrix * vec4(position.xyz, 1.0) * orthographicMatrix;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";

    public static final String kMVYGPUImageTransitionFragmentShaderString = "" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform lowp float transparent;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "    gl_FragColor = vec4(textureColor.rgb, (textureColor.w * transparent));\n" +
            "}";

    public MVYGPUImageFramebuffer tempFramebuffer;
    public MVYGPUImageFramebuffer outputFramebuffer;

    protected MVYGLProgram filterProgram;

    protected int filterPositionAttribute, filterTextureCoordinateAttribute;
    protected int filterInputTextureUniform;
    protected int transformMatrixUniform;
    protected int orthographicMatrixUniform;
    protected int transparentUniform;

    private boolean anchorTopLeft = false;

    // 缓存的纹理数据
    private List<MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel> cacheTextures = new ArrayList<>();

    // 渲染的纹理数据
    private List<MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel> renderTextures = new ArrayList<>();

    // 图片缩放方式
    private MVYGPUImageConstants.MVYGPUImageContentMode contentMode = MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFill;

    public MVYGPUImageTextureTransitionInput() {

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram = new MVYGLProgram(kMVYGPUImageTransitionVertexShaderString, kMVYGPUImageTransitionFragmentShaderString);
                filterProgram.link();

                filterPositionAttribute = filterProgram.attributeIndex("position");
                filterTextureCoordinateAttribute = filterProgram.attributeIndex("inputTextureCoordinate");
                filterInputTextureUniform = filterProgram.uniformIndex("inputImageTexture");
                transformMatrixUniform = filterProgram.uniformIndex("transformMatrix");
                orthographicMatrixUniform = filterProgram.uniformIndex("orthographicMatrix");
                transparentUniform = filterProgram.uniformIndex("transparent");
                filterProgram.use();
            }
        });
    }

    /**
     * 设置图片缩放方式
     */
    public void setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode contentMode) {
        this.contentMode = contentMode;
    }

    /**
     * 渲染
     */
    public void process(final int boundingWidth, final int boundingHeight) {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {

                int inputWidth = boundingWidth;
                int inputHeight = boundingHeight;

                filterProgram.use();

                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                if (outputFramebuffer != null) {
                    if (inputWidth != outputFramebuffer.width || inputHeight != outputFramebuffer.height) {
                        outputFramebuffer.destroy();
                        outputFramebuffer = null;

                        tempFramebuffer.destroy();
                        tempFramebuffer = null;
                    }
                }

                if (outputFramebuffer == null) {
                    outputFramebuffer = new MVYGPUImageFramebuffer(inputWidth, inputHeight);

                    tempFramebuffer = new MVYGPUImageFramebuffer(inputWidth, inputHeight);
                }

                outputFramebuffer.activateFramebuffer();

                glClearColor(0, 0, 0, 1);
                glClear(GL_COLOR_BUFFER_BIT);

                for (MVYGPUImageTextureModel textureModel: renderTextures) {

                    tempFramebuffer.activateFramebuffer();

                    glClearColor(0, 0, 0, 1);
                    glClear(GL_COLOR_BUFFER_BIT);

                    glActiveTexture(GL_TEXTURE2);

                    glBindTexture(GL_TEXTURE_2D, textureModel.texture[0]);

                    glUniform1i(filterInputTextureUniform, 2);

                    float[] transformMatrix = new float[16];
                    Matrix.setIdentityM(transformMatrix, 0);
                    Matrix.rotateM(transformMatrix, 0, 180.f, 1.f, 0.f, 0.f);
                    glUniformMatrix4fv(transformMatrixUniform, 1, false, transformMatrix, 0);

                    float[] orthographicMatrix = new float[16];
                    loadOrthoMatrix(orthographicMatrix, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);
                    glUniformMatrix4fv(orthographicMatrixUniform, 1, false, orthographicMatrix, 0);

                    glUniform1f(transparentUniform, textureModel.transparent);

                    PointF insetSize = MVYGPUImageConstants.getAspectRatioInsideSize(new PointF(textureModel.width, textureModel.height), new PointF(boundingWidth, boundingHeight));
                    if (MVYGPUImageConstants.needExchangeWidthAndHeightWithRotation(textureModel.rotation)) {
                        insetSize = MVYGPUImageConstants.getAspectRatioInsideSize(new PointF(textureModel.height, textureModel.width), new PointF(boundingWidth, boundingHeight));
                    }

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

                    float[] textureCoordinates = MVYGPUImageConstants.textureCoordinatesForRotation(textureModel.rotation);

                    glEnableVertexAttribArray(filterPositionAttribute);
                    glEnableVertexAttribArray(filterTextureCoordinateAttribute);

                    glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0,  MVYGPUImageConstants.floatArrayToBuffer(squareVertices));
                    glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, MVYGPUImageConstants.floatArrayToBuffer(textureCoordinates));

                    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                    outputFramebuffer.activateFramebuffer();

                    glActiveTexture(GL_TEXTURE2);
                    glBindTexture(GL_TEXTURE_2D, tempFramebuffer.texture[0]);
                    glUniform1i(filterInputTextureUniform, 2);

                    transformMatrix = textureModel.transformMatrix.clone();
                    glUniformMatrix4fv(transformMatrixUniform, 1, false, transformMatrix, 0);

                    orthographicMatrix = new float[16];
                    loadOrthoMatrix(orthographicMatrix, -1.0f, 1.0f, -1.0f * inputHeight / inputWidth, 1.0f * inputHeight / inputWidth, -1.0f, 1.0f);
                    glUniformMatrix4fv(orthographicMatrixUniform, 1, false, orthographicMatrix, 0);

                    glUniform1f(transparentUniform, textureModel.transparent);

                    float normalizedHeight = (float) inputHeight / (float) inputWidth;
                    float adjustedVertices[] = {
                            -1.0f, -normalizedHeight,
                            1.0f, -normalizedHeight,
                            -1.0f,  normalizedHeight,
                            1.0f,  normalizedHeight,
                    };
                    textureCoordinates = MVYGPUImageConstants.textureCoordinatesForRotation(kMVYGPUImageNoRotation);

                    glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0,  MVYGPUImageConstants.floatArrayToBuffer(adjustedVertices));
                    glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, MVYGPUImageConstants.floatArrayToBuffer(textureCoordinates));

                    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                    glDisableVertexAttribArray(filterPositionAttribute);
                    glDisableVertexAttribArray(filterTextureCoordinateAttribute);
                }

                glDisable(GL_BLEND);

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

    /**
     * 添加缓存Texture
     */
    public void addCacheTexture(final MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel textureModel) {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                cacheTextures.add(textureModel);
            }
        });
    }

    /**
     * 移除缓存Texture
     */
    public void removeCacheTexture(final MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel textureModel) {
        if (textureModel == null) return;
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                textureModel.destroy();

                cacheTextures.remove(textureModel);
                renderTextures.remove(textureModel);
            }
        });
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                for (MVYGPUImageTextureModel textureModel: cacheTextures) {
                    textureModel.destroy();
                }
                cacheTextures.clear();
                renderTextures.clear();
            }
        });
    }

    /**
     * 获取缓存Texture
     */
    public List<MVYGPUImageTextureModel> getCacheTextures() {
        return cacheTextures;
    }

    /**
     * 设置渲染Texture
     */
    public void setRenderTextures(List<MVYGPUImageTextureModel> renderTextures) {
        this.renderTextures = renderTextures;
    }

    /**
     * 获取渲染Texture
     */
    public List<MVYGPUImageTextureModel> getRenderTextures() {
        return renderTextures;
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
                if (tempFramebuffer != null) {
                    tempFramebuffer.destroy();
                }
            }
        });

        clearCache();
    }
    
    public void loadOrthoMatrix(float[] matrix, float left, float right, float bottom, float top, float near, float far) {
        float r_l = right - left;
        float t_b = top - bottom;
        float f_n = far - near;
        float tx = - (right + left) / (right - left);
        float ty = - (top + bottom) / (top - bottom);
        float tz = - (far + near) / (far - near);

        float scale = 2.0f;
        if (anchorTopLeft) {
            scale = 4.0f;
            tx=-1.0f;
            ty=-1.0f;
        }

        matrix[0] = scale / r_l;
        matrix[1] = 0.0f;
        matrix[2] = 0.0f;
        matrix[3] = tx;

        matrix[4] = 0.0f;
        matrix[5] = scale / t_b;
        matrix[6] = 0.0f;
        matrix[7] = ty;

        matrix[8] = 0.0f;
        matrix[9] = 0.0f;
        matrix[10] = scale / f_n;
        matrix[11] = tz;

        matrix[12] = 0.0f;
        matrix[13] = 0.0f;
        matrix[14] = 0.0f;
        matrix[15] = 1.0f;
    }


    public static class MVYGPUImageTextureModel {
        public int[] texture = new int[1];
        public int width;
        public int height;
        public float[] transformMatrix = new float[16];
        public float transparent = 1;
        public MVYGPUImageConstants.MVYGPUImageRotationMode rotation = kMVYGPUImageNoRotation;

        /**
         * 创建一个纹理贴图对象
         */
        public MVYGPUImageTextureModel(final Bitmap bitmap){
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
