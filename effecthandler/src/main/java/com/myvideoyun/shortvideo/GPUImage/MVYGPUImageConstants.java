package com.myvideoyun.shortvideo.GPUImage;

import android.graphics.PointF;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by 汪洋 on 2018/12/8.
 * Copyright © 2018年 myvideoyun. All rights reserved.
 */
public class MVYGPUImageConstants {
    public static final String TAG = "MVYGPUImage";

    public static float imageVertices[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f,  1.0f,
    };

    public enum MVYGPUImageRotationMode {
        kMVYGPUImageNoRotation,
        kMVYGPUImageRotateLeft,
        kMVYGPUImageRotateRight,
        kMVYGPUImageFlipVertical,
        kMVYGPUImageFlipHorizontal,
        kMVYGPUImageRotateRightFlipVertical,
        kMVYGPUImageRotateRightFlipHorizontal,
        kMVYGPUImageRotate180
    }

    public enum MVYGPUImageContentMode {
        kMVYGPUImageScaleToFill, //图像填充方式一:拉伸
        kMVYGPUImageScaleAspectFit, //图像填充方式二:保持宽高比
        kMVYGPUImageScaleAspectFill //图像填充方式三:保持宽高比同时填满整个屏幕
    }

    public static float noRotationTextureCoordinates[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };

    public static float rotateLeftTextureCoordinates[] = {
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
    };

    public static float rotateRightTextureCoordinates[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
    };

    public static float verticalFlipTextureCoordinates[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f,  0.0f,
            1.0f,  0.0f,
    };

    public static float horizontalFlipTextureCoordinates[] = {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f,  1.0f,
            0.0f,  1.0f,
    };

    public static float rotateRightVerticalFlipTextureCoordinates[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    public static float rotateRightHorizontalFlipTextureCoordinates[] = {
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
    };

    public static float rotate180TextureCoordinates[] = {
            1.0f, 1.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
    };

    public static float[] textureCoordinatesForRotation(MVYGPUImageRotationMode rotationMode) {
        switch(rotationMode) {
            case kMVYGPUImageNoRotation: return noRotationTextureCoordinates;
            case kMVYGPUImageRotateLeft: return rotateLeftTextureCoordinates;
            case kMVYGPUImageRotateRight: return rotateRightTextureCoordinates;
            case kMVYGPUImageFlipVertical: return verticalFlipTextureCoordinates;
            case kMVYGPUImageFlipHorizontal: return horizontalFlipTextureCoordinates;
            case kMVYGPUImageRotateRightFlipVertical: return rotateRightVerticalFlipTextureCoordinates;
            case kMVYGPUImageRotateRightFlipHorizontal: return rotateRightHorizontalFlipTextureCoordinates;
            case kMVYGPUImageRotate180: return rotate180TextureCoordinates;
            default:return noRotationTextureCoordinates;
        }
    }

    public static boolean needExchangeWidthAndHeightWithRotation(MVYGPUImageRotationMode rotationMode) {
        switch(rotationMode)
        {
            case kMVYGPUImageNoRotation: return false;
            case kMVYGPUImageRotateLeft: return true;
            case kMVYGPUImageRotateRight: return true;
            case kMVYGPUImageFlipVertical: return false;
            case kMVYGPUImageFlipHorizontal: return false;
            case kMVYGPUImageRotateRightFlipVertical: return true;
            case kMVYGPUImageRotateRightFlipHorizontal: return true;
            case kMVYGPUImageRotate180: return false;
            default:return false;
        }
    }

    public static FloatBuffer floatArrayToBuffer(float[] array) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(array.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(array);
        floatBuffer.position(0);
        return floatBuffer;
    }

    public static PointF getAspectRatioInsideSize(PointF sourceSize, PointF boundingSize) {
        float sourceRatio = sourceSize.x / sourceSize.y;
        float boundingRatio = boundingSize.x / boundingSize.y;

        PointF destRatio = new PointF(0, 0);

        if (sourceRatio < boundingRatio) {
            destRatio.x = sourceRatio * boundingSize.y;
            destRatio.y = boundingSize.y;

        } else if (sourceRatio > boundingRatio) {
            destRatio.x = boundingSize.x;
            destRatio.y = boundingSize.x / sourceRatio;

        } else {
            destRatio = boundingSize;
        }

        return destRatio;
    }

    public static float[] matrix4fvMult (float[] a, float[] b) {
        int x, y;
        float temp[] = new float[16];

        for(y=0; y<4 ; y++) {
            for(x=0 ; x<4 ; x++) {
                temp[4 * y + x] = b[4 * y] * a[x]
                        + b[4 * y + 1] * a[4 + x]
                        + b[4 * y + 2] * a[8 + x]
                        + b[4 * y + 3] * a[12 + x];
            }
        }

        return temp;
    }
}