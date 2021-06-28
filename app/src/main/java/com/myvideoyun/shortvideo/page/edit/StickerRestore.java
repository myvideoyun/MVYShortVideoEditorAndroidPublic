package com.myvideoyun.shortvideo.page.edit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.Matrix;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.myvideoyun.decoder.VideoFrame;
import com.myvideoyun.shortvideo.MVYApplication;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageStickerFilter;
import com.myvideoyun.shortvideo.MVYVideoEffectHandler;
import com.myvideoyun.shortvideo.model.StickerModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StickerRestore {
    public static Map<View, StickerModel> models = new HashMap<>();
    public static List<StickerModel> modelList = new ArrayList<>();

    /**
     * 添加贴纸大到视频中
     */
    public static void addAllStickerToVideo(MVYVideoEffectHandler effectHandler, VideoFrame frame){
        for (StickerModel model : models.values()) {
            if (frame.pts >= model.startTime && frame.pts <= model.endTime){
                // 在需要显示的区间内
                if (model.imageStickerModel == null){
                    Bitmap bitmap = null;
                    try {
                        bitmap = BitmapFactory.decodeStream(MVYApplication.instance.getAssets().open(model.path));
                    } catch (IOException e) {
                        e.printStackTrace();
                        try {
                            // bitmap 存在非assets
                            bitmap = BitmapFactory.decodeFile(model.path);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (bitmap != null) {
                        model.imageStickerModel = new MVYGPUImageStickerFilter.MVYGPUImageStickerModel(bitmap);
//                        Matrix.rotateM(model.imageStickerModel.transformMatrix, 0, 0.f, 0.f, 0.f, 1.f);

                        Matrix.rotateM(model.imageStickerModel.transformMatrix, 0, 0.f, 0.f, 0.f, 1.f);
                        model.originalMatrix = model.imageStickerModel.transformMatrix;

                        Matrix.translateM(model.imageStickerModel.transformMatrix, 0, model.x, model.y, 0.f);
                        Matrix.scaleM(model.imageStickerModel.transformMatrix, 0, model.scale, model.scale, 0f);
                    }
                }
                if (!model.isAdded){
                    model.isAdded = true;
                    effectHandler.addSticker(model.imageStickerModel);
                    if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                        model.view.setVisibility(View.VISIBLE);
                    }
                    Log.e("moose", "添加贴纸：" +  model);
                }
            } else {
                effectHandler.removeSticker(model.imageStickerModel);
                if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                    model.view.setVisibility(View.INVISIBLE);
                }
                model.isAdded = false;
                model.imageStickerModel = null;
            }
        }
    }

    public static StickerModel add(String path, View view) {
        MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerFilterModel;
        // 添加贴纸
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(MVYApplication.instance.getAssets().open(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bitmap != null) {
            stickerFilterModel = new MVYGPUImageStickerFilter.MVYGPUImageStickerModel(bitmap);

            // 设置贴纸位置
            StickerModel model = new StickerModel();
            model.view = view;
            model.imageStickerModel = stickerFilterModel;
            Matrix.rotateM(stickerFilterModel.transformMatrix, 0, 0.f, 0.f, 0.f, 1.f);
            model.originalMatrix = stickerFilterModel.transformMatrix;
            model.path = path;
            models.put(view, model);
            return model;
        }
        return null;
    }

    /**
     * 添加新的字幕到屏幕
     * @param path
     * @param view
     * @return
     */
    public static StickerModel addSubtitle(String path, View view) {
        MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerFilterModel;
        // 添加贴纸
        Bitmap bitmap = null;
        bitmap = BitmapFactory.decodeFile(path);
        if (bitmap != null) {
            stickerFilterModel = new MVYGPUImageStickerFilter.MVYGPUImageStickerModel(bitmap);

            // 设置贴纸位置
            StickerModel model = new StickerModel();
            model.view = view;
            model.imageStickerModel = stickerFilterModel;
            Matrix.rotateM(stickerFilterModel.transformMatrix, 0, 0.f, 0.f, 0.f, 1.f);
            model.originalMatrix = stickerFilterModel.transformMatrix;
            model.path = path;
            models.put(view, model);
            return model;
        }
        return null;
    }

    /**
     * 设置贴纸显示时间
     * @param model
     * @param start
     * @param end
     */
    public static void setTime(StickerModel model, float start, float end){
        if (model != null){
            model.startTime = start;
            model.endTime = end;
        }
    }
    /**
     * 设置贴纸显示时间
     * @param view
     * @param start
     * @param end
     */
    public static void setTime(View view, float start, float end){
        StickerModel model = models.get(view);
        if (view != null){
            model.startTime = start;
            model.endTime = end;

        }
    }

    public static StickerModel get(View view) {
        return models.get(view);
    }

    public static void setStickerTranslateScale(View view, float realX, float realY, float scale) {
        StickerModel model = StickerRestore.get(view);
        if (model != null && model.imageStickerModel != null && model.isAdded) {
            // 初始化上次的矩阵
            Matrix.setIdentityM(model.originalMatrix, 0);
            Matrix.translateM(model.imageStickerModel.transformMatrix, 0, realX, realY, 0.f);
            Matrix.scaleM(model.imageStickerModel.transformMatrix, 0, scale, scale, 0f);
            model.x = realX;
            model.y = realY;
            model.scale = scale;
        }
    }

    public static void clear() {
        models.clear();
        modelList.clear();
    }

    public static void initAdded(){
        for (StickerModel model : models.values()) {
            model.isAdded = false;
        }
    }
}
