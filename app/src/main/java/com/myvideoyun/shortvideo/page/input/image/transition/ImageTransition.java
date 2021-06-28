package com.myvideoyun.shortvideo.page.input.image.transition;

import android.opengl.Matrix;

import com.myvideoyun.shortvideo.GPUImageCustomFilter.inputOutput.MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel;

import java.util.ArrayList;
import java.util.List;

public class ImageTransition {

    // 计算从左到右转场效果
    static List<MVYGPUImageTextureModel> leftToRight(List<MVYGPUImageTextureModel> cacheTextures, int renderIndex, int frameCountOfPreTexture) {

        List<MVYGPUImageTextureModel> renderTexture = new ArrayList<>();

        float processCount = frameCountOfPreTexture / 2;

        // 当前帧
        int currentIndex = renderIndex / frameCountOfPreTexture % cacheTextures.size();
        MVYGPUImageTextureModel textureModel = cacheTextures.get(currentIndex);
        Matrix.setIdentityM(textureModel.transformMatrix, 0);
        if (renderIndex % frameCountOfPreTexture < processCount && currentIndex > 0) {
            Matrix.translateM(textureModel.transformMatrix, 0, 2.0f - (renderIndex % frameCountOfPreTexture / processCount) * 2.0f, 0, 0);
        }
        textureModel.transparent = 1;

        // 上一帧
        if (renderIndex % frameCountOfPreTexture < processCount && currentIndex > 0) {
            int previousIndex = currentIndex - 1;

            MVYGPUImageTextureModel textureModel2 = cacheTextures.get(previousIndex);
            Matrix.setIdentityM(textureModel2.transformMatrix, 0);
            Matrix.translateM(textureModel2.transformMatrix, 0, 0.0f - (renderIndex % frameCountOfPreTexture / processCount) * 2, 0, 0);
            textureModel2.transparent = 1;
            renderTexture.add(textureModel2);
        }

        renderTexture.add(textureModel);

        return renderTexture;
    }

    // 计算从上到下转场效果
    static List<MVYGPUImageTextureModel> topToBottom(List<MVYGPUImageTextureModel> cacheTextures, int renderIndex, int frameCountOfPreTexture) {

        List<MVYGPUImageTextureModel> renderTexture = new ArrayList<>();

        float processCount = frameCountOfPreTexture / 2;

        // 当前帧
        int currentIndex = renderIndex / frameCountOfPreTexture % cacheTextures.size();
        MVYGPUImageTextureModel textureModel = cacheTextures.get(currentIndex);
        Matrix.setIdentityM(textureModel.transformMatrix, 0);
        if (renderIndex % frameCountOfPreTexture < processCount && currentIndex > 0) {
            Matrix.translateM(textureModel.transformMatrix, 0, 0, (renderIndex % frameCountOfPreTexture / processCount) * 3.4f - 3.4f, 0);
        }
        textureModel.transparent = 1;

        // 上一帧
        if (renderIndex % frameCountOfPreTexture < processCount && currentIndex > 0) {
            int previousIndex = currentIndex - 1;

            MVYGPUImageTextureModel textureModel2 = cacheTextures.get(previousIndex);
            Matrix.setIdentityM(textureModel2.transformMatrix, 0);
            Matrix.translateM(textureModel2.transformMatrix, 0, 0, (renderIndex % frameCountOfPreTexture / processCount) * 3.4f, 0);
            textureModel2.transparent = 1;
            renderTexture.add(textureModel2);
        }

        renderTexture.add(textureModel);

        return renderTexture;
    }

    // 计算放大转场效果
    static List<MVYGPUImageTextureModel> zoomOut(List<MVYGPUImageTextureModel> cacheTextures, int renderIndex, int frameCountOfPreTexture) {

        List<MVYGPUImageTextureModel> renderTexture = new ArrayList<>();

        // 当前帧
        int currentIndex = renderIndex / frameCountOfPreTexture % cacheTextures.size();
        MVYGPUImageTextureModel textureModel = cacheTextures.get(currentIndex);
        Matrix.setIdentityM(textureModel.transformMatrix, 0);
        Matrix.scaleM(textureModel.transformMatrix, 0, renderIndex % frameCountOfPreTexture / (frameCountOfPreTexture * 2.0f) + 1.0f, renderIndex % frameCountOfPreTexture / (frameCountOfPreTexture * 2.0f) + 1.0f, 1);
        textureModel.transparent = 1;

        renderTexture.add(textureModel);

        return renderTexture;
    }

    // 计算缩小转场效果
    static List<MVYGPUImageTextureModel> zoomIn(List<MVYGPUImageTextureModel> cacheTextures, int renderIndex, int frameCountOfPreTexture) {

        List<MVYGPUImageTextureModel> renderTexture = new ArrayList<>();

        // 当前帧
        int currentIndex = renderIndex / frameCountOfPreTexture % cacheTextures.size();
        MVYGPUImageTextureModel textureModel = cacheTextures.get(currentIndex);
        Matrix.setIdentityM(textureModel.transformMatrix, 0);
        Matrix.scaleM(textureModel.transformMatrix, 0, 1.5f - (renderIndex % frameCountOfPreTexture / (frameCountOfPreTexture * 2.0f)), 1.5f - (renderIndex % frameCountOfPreTexture / (frameCountOfPreTexture * 2.0f)), 1);
        textureModel.transparent = 1;

        renderTexture.add(textureModel);

        return renderTexture;
    }

    // 计算旋转同时缩小转场效果
    static List<MVYGPUImageTextureModel> rotateAndZoomIn(List<MVYGPUImageTextureModel> cacheTextures, int renderIndex, int frameCountOfPreTexture) {

        List<MVYGPUImageTextureModel> renderTexture = new ArrayList<>();

        float processCount = frameCountOfPreTexture / 2;

        // 当前帧
        int currentIndex = renderIndex / frameCountOfPreTexture % cacheTextures.size();
        MVYGPUImageTextureModel textureModel = cacheTextures.get(currentIndex);
        Matrix.setIdentityM(textureModel.transformMatrix, 0);
        textureModel.transparent = 1;
        renderTexture.add(textureModel);

            // 上一帧
        if (renderIndex % frameCountOfPreTexture < processCount && currentIndex > 0) {
            int previousIndex = currentIndex - 1;
            MVYGPUImageTextureModel textureModel2 = cacheTextures.get(previousIndex);
            Matrix.setIdentityM(textureModel2.transformMatrix, 0);
            Matrix.scaleM(textureModel2.transformMatrix, 0, 1f - (renderIndex % processCount / (processCount * 1.0f)), 1f - (renderIndex % processCount / (processCount * 1.0f)), 1);
            Matrix.rotateM(textureModel2.transformMatrix, 0, (renderIndex % processCount  / (processCount * 1.0f) * 360), 0, 0, 1);
            textureModel2.transparent = 1;
            renderTexture.add(textureModel2);
        }

        return renderTexture;
    }

    // 过渡
    static List<MVYGPUImageTextureModel> transparent(List<MVYGPUImageTextureModel> cacheTextures, int renderIndex, int frameCountOfPreTexture) {

        List<MVYGPUImageTextureModel> renderTexture = new ArrayList<>();

        float processCount = frameCountOfPreTexture / 2;

        // 当前帧
        int currentIndex = renderIndex / frameCountOfPreTexture % cacheTextures.size();
        MVYGPUImageTextureModel textureModel = cacheTextures.get(currentIndex);
        Matrix.setIdentityM(textureModel.transformMatrix, 0);
        if (renderIndex % frameCountOfPreTexture < processCount && currentIndex > 0) {
            textureModel.transparent = renderIndex % frameCountOfPreTexture  / processCount;
        } else {
            textureModel.transparent = 1;
        }

        // 上一帧
        if (currentIndex > 0) {
            int previousIndex = currentIndex - 1;
            MVYGPUImageTextureModel textureModel2 = cacheTextures.get(previousIndex);
            Matrix.setIdentityM(textureModel2.transformMatrix, 0);
            if (renderIndex % frameCountOfPreTexture < processCount) {
                textureModel2.transparent = 1;
            } else {
                textureModel2.transparent = 0;
            }
            renderTexture.add(textureModel2);
        }

        renderTexture.add(textureModel);

        return renderTexture;
    }
}
