package com.myvideoyun.shortvideo.recordTool;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.util.Log;
import android.view.MotionEvent;

import com.myvideoyun.shortvideo.MVYCameraEffectHandler;
import com.myvideoyun.shortvideo.GPUImage.MVYGLProgram;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFilter;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFramebuffer;
import com.myvideoyun.shortvideo.tools.ScreenTools;

import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES20.*;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageNoRotation;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.needExchangeWidthAndHeightWithRotation;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.textureCoordinatesForRotation;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext.syncRunOnRenderThread;

/*
 * Created by 汪洋 on 2019/2/11.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class MVYCameraPreviewWrap implements SurfaceTexture.OnFrameAvailableListener {
    public static final String TAG = "MVYCameraPreviewWrap";

    public static final String kMVYOESTextureFragmentShader = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    private Camera mCamera;

    private MVYGPUImageEGLContext eglContext = new MVYGPUImageEGLContext();

    private SurfaceTexture surfaceTexture;

    private int oesTexture;

    private MVYCameraPreviewListener previewListener;

    private MVYGPUImageFramebuffer outputFramebuffer;

    private MVYGPUImageConstants.MVYGPUImageRotationMode rotateMode = MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageNoRotation;

    private int inputWidth;
    private int inputHeight;

    private MVYGLProgram filterProgram;

    private int filterPositionAttribute, filterTextureCoordinateAttribute;
    private int filterInputTextureUniform;

    private Buffer imageVertices = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.imageVertices);
    private Buffer textureCoordinates = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.noRotationTextureCoordinates);

    //
    private long lastTouchTime = 0;
    private int lastTouchDistance = 0;
    private MVYCameraEffectHandler effectHandler;

    private MVYCameraPreviewWrap() {}

    public MVYCameraPreviewWrap(Camera camera) {
        mCamera = camera;
        init();
        setAutoFocus();
//        setCameraSize();
    }

    public void setCamera(Camera camera){
        mCamera = camera;
        init();
        setAutoFocus();
//        setCameraSize();
    }

    public void setCameraSize(double scale) {
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            for (int i = 0 ; i < sizes.size() ; i++ ){
                Camera.Size size = sizes.get(i);
                float tempScale = (float) size.height / (float) size.width;
                Log.e(TAG, "支持比例：width = " + size.width + ", height = " + size.height + ", tempScale = " + tempScale + "， needScale = " + scale);
                if (scale == tempScale){
                    parameters.setPreviewSize(size.width, size.height);
                    Log.e(TAG, "设置的比例是：" + tempScale + " , width = " + size.width + " , height = " + size.height);
                    break;
                }
            }
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "设置预览比例出错: " + e.getMessage());
        }
    }

    private void init() {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                eglContext.initEGLWindow(new SurfaceTexture(0));

                oesTexture = createOESTextureID();
                surfaceTexture = new SurfaceTexture(oesTexture);
                surfaceTexture.setOnFrameAvailableListener(MVYCameraPreviewWrap.this);

                filterProgram = new MVYGLProgram(MVYGPUImageFilter.kMVYGPUImageVertexShaderString, kMVYOESTextureFragmentShader);
                filterProgram.link();

                filterPositionAttribute = filterProgram.attributeIndex("position");
                filterTextureCoordinateAttribute = filterProgram.attributeIndex("inputTextureCoordinate");
                filterInputTextureUniform = filterProgram.uniformIndex("inputImageTexture");
                filterProgram.use();
            }
        });
    }

    public void startPreview() {
        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException ignored) {
        }

        Camera.Size s = mCamera.getParameters().getPreviewSize();
        inputWidth = s.width;
        inputHeight = s.height;

        setRotateMode(rotateMode);

        mCamera.startPreview();
   }

   public void stopPreview() {
       destroy();
       mCamera.stopPreview();
   }
   
    @Override
    public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                eglContext.makeCurrent();

                surfaceTexture.updateTexImage();

                glFinish();

                // 因为在shader中处理oes纹理需要使用到扩展类型, 必须要先转换为普通纹理再传给下一级
                renderToFramebuffer(oesTexture);

                if (previewListener != null) {
                    previewListener.cameraVideoOutput(outputFramebuffer.texture[0], inputWidth, inputHeight, surfaceTexture.getTimestamp());
                }
            }
        });
    }

    private void renderToFramebuffer(int oesTexture) {

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
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTexture);

        glUniform1i(filterInputTextureUniform, 2);

        glEnableVertexAttribArray(filterPositionAttribute);
        glEnableVertexAttribArray(filterTextureCoordinateAttribute);

        glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, imageVertices);
        glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.textureCoordinatesForRotation(rotateMode)));

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glDisableVertexAttribArray(filterPositionAttribute);
        glDisableVertexAttribArray(filterTextureCoordinateAttribute);
    }

    public void setPreviewListener(MVYCameraPreviewListener previewListener) {
        this.previewListener = previewListener;
    }

    public void setRotateMode(MVYGPUImageConstants.MVYGPUImageRotationMode rotateMode) {
        this.rotateMode = rotateMode;

        if (MVYGPUImageConstants.needExchangeWidthAndHeightWithRotation(rotateMode)) {
            int temp = inputWidth;
            inputWidth = inputHeight;
            inputHeight = temp;
        }
    }

    private int createOESTextureID() {
        int[] texture = new int[1];
        glGenTextures(1, texture, 0);
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_TEXTURE_MIN_FILTER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_TEXTURE_MAG_FILTER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_TEXTURE_WRAP_S);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_TEXTURE_WRAP_T);

        return texture[0];
    }

    public void destroy() {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {

                filterProgram.destroy();

                if (outputFramebuffer != null) {
                    outputFramebuffer.destroy();
                }

                eglContext.destroyEGLWindow();
            }
        });
    }


    /**
     * 手动触摸对焦：todo 没有效果
     * @param event
     */
    public void touchingFocus(MotionEvent event, Activity activity) {
        Point screen = ScreenTools.getScreen(activity);
        int x = (int) (event.getRawX() / screen.x * 2000 - 1000);
        int y = (int) (event.getRawY() / screen.y * 2000 - 1000);
//        int x = (int) (event.getRawX() - screen.x / 2);
//        int y = (int) (event.getRawY() - screen.y / 2);
        int left = Math.max(x - 100, -1000);
        int right = Math.min(x + 100, 1000);
        int top = Math.max(y - 100, -1000);
        int bottom = Math.min(y + 100, 1000);
        Log.e(TAG, "对焦区域：left="+ left + ", top="+ top + ", rigth="+ right+ ", bottom="+ bottom);
        if (mCamera != null){
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getMaxNumFocusAreas() > 0) {
                ArrayList<Camera.Area> areas = new ArrayList<>();
                areas.add(new Camera.Area(new Rect(left, top, right, bottom), 800));
                parameters.setFocusAreas(areas);
                try {
                    mCamera.cancelAutoFocus();
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            Log.e(TAG, "focus result = " + success);
                            setAutoFocus();
                        }
                    });
                    mCamera.setParameters(parameters);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(this.getClass().getSimpleName(), "do not support focus areas.");
            }
        }
    }

    public boolean onTouchEvent(MotionEvent event, Activity activity) {
        long currTime = System.currentTimeMillis();
        long interval = currTime - lastTouchTime;
        if (interval > 200){
            lastTouchTime = currTime;
            touchingFocus(event, activity);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchDistance = 0;
                    break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() >=2){
                    int offsetX = (int) (event.getX(0) - event.getX(1));
                    int offsetY = (int) (event.getY(0) - event.getY(1));
                    int currentDistance = (int) Math.sqrt(offsetX * offsetX + offsetY * offsetY);

                    if (lastTouchDistance == 0){
                        lastTouchDistance = currentDistance;
                        break;
                    }

                    int touchScale = currentDistance - lastTouchDistance;
                    zoomCamera(touchScale);
                    lastTouchDistance = currentDistance;
                }
                break;
                case MotionEvent.ACTION_UP:
                    lastTouchDistance = 0;
                    break;
        }
        return false;
    }

    private int maxZoom = 8;// 最大8倍变焦
    /**
     * 设置变焦参数
     * @param touchScale
     */
    private void zoomCamera(double touchScale) {
        float originalZoom = effectHandler.getZoom();
        double zoom = touchScale / 100 + originalZoom;
        if (zoom > 1){
            zoom = Math.min(zoom, maxZoom);
        } else if (zoom <= 1){
            zoom = Math.max(zoom, 1);// 最小1倍
        }
        if (effectHandler != null){
            effectHandler.setZoom((float) zoom);
        }
        Log.e(TAG, "touchScale=" + touchScale + "，originalZoom = " + originalZoom + ", new zoom = " + zoom);
    }
    /**
     * 设置变焦参数
     * @param touchScale
     */
    private void zoomCameraFromSystem(int touchScale) {
        Camera.Parameters parameters = mCamera.getParameters();
        int zoom = parameters.getZoom();
        int maxZoom = parameters.getMaxZoom();
        if (touchScale > 0){
            zoom = Math.min(++zoom, maxZoom);
        } else if (touchScale < 0){
            zoom = Math.max(--zoom, 0);
        }
        parameters.setZoom(zoom);
        mCamera.setParameters(parameters);
        Log.e(TAG, "touchScale=" + touchScale + "zoom = " + zoom + ", max zoom = " + maxZoom);
    }


    /**
     * 切换闪光灯模式
     */
    public void switchCameraFlashMode(){
        if (mCamera != null){
            Camera.Parameters parameters = mCamera.getParameters();
            String flashMode = parameters.getFlashMode();

            if (Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                Log.d(TAG, "关闭闪光灯");
            } else if (Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)){
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                Log.d(TAG, "打开闪光灯");
            }
            mCamera.setParameters(parameters);
        }
    }

    /**
     * 设置自动对焦
     */
    private void setAutoFocus() {
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setEffetHandler(MVYCameraEffectHandler effectHandler) {
        this.effectHandler =  effectHandler;
    }
}

