package com.myvideoyun.shortvideo;

import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFilter;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.inputOutput.MVYGPUImageTextureOutput;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.inputOutput.MVYGPUImageTextureTransitionInput;

import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_FRAMEBUFFER_BINDING;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_RENDERBUFFER;
import static android.opengl.GLES20.GL_RENDERBUFFER_BINDING;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_VERTEX_ATTRIB_ARRAY_ENABLED;
import static android.opengl.GLES20.GL_VIEWPORT;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindRenderbuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glGetVertexAttribiv;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glViewport;

public class MVYImageEffectHandler {

    private int[] outputTexture = {0};

    private MVYGPUImageTextureTransitionInput textureTransitionInput;
    private MVYGPUImageTextureOutput textureOutput;

    private MVYGPUImageFilter commonInputFilter;
    private MVYGPUImageFilter commonOutputFilter;

    private boolean initCommonProcess = false;
    private boolean initProcess = false;

    private int[] bindingFrameBuffer = new int[1];
    private int[] bindingRenderBuffer = new int[1];
    private int[] viewPoint = new int[4];
    private int vertexAttribEnableArraySize = 5;
    private ArrayList<Integer> vertexAttribEnableArray = new ArrayList(vertexAttribEnableArraySize);

    public int outputWidth;
    public int outputHeight;

    public MVYImageEffectHandler() {

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable(){
            @Override
            public void run() {

                if (outputTexture[0] == 0) {
                    glGenTextures(1, outputTexture, 0);
                    glBindTexture(GL_TEXTURE_2D, outputTexture[0]);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                    glBindTexture(GL_TEXTURE_2D, 0);
                }

                textureTransitionInput = new MVYGPUImageTextureTransitionInput();
                textureOutput = new MVYGPUImageTextureOutput();

                commonInputFilter = new MVYGPUImageFilter();
                commonOutputFilter = new MVYGPUImageFilter();
            }
        });
    }

    private void commonProcess() {

        List<MVYGPUImageFilter> filterChainArray = new ArrayList<MVYGPUImageFilter>();

        if (!initCommonProcess) {

            if (filterChainArray.size() > 0) {
                commonInputFilter.addTarget(filterChainArray.get(0));
                for (int x = 0; x < filterChainArray.size() - 1; x++) {
                    filterChainArray.get(x).addTarget(filterChainArray.get(x+1));
                }
                filterChainArray.get(filterChainArray.size()-1).addTarget(commonOutputFilter);

            }else {
                commonInputFilter.addTarget(commonOutputFilter);
            }

            initCommonProcess = true;
        }
    }

    public int process(final int width, final int height) {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {

//                saveOpenGLState();

//                commonProcess();

//                if (!initProcess) {
//                    textureTransitionInput.addTarget(commonInputFilter);
//                    commonOutputFilter.addTarget(textureOutput);
//                    initProcess = true;
//                }

                outputWidth = width;
                outputHeight = height;

//                textureOutput.setOutputWithBGRATexture(outputTexture[0], width, height);

                // ???????????????Filter, ??????????????????????????????
                textureTransitionInput.process(width, height);

//                restoreOpenGLState();
            }
        });

        return textureTransitionInput.outputFramebuffer.texture[0];
//        return outputTexture[0];
    }

    private void saveOpenGLState() {
        // ?????????????????????FrameBuffer
        glGetIntegerv(GL_FRAMEBUFFER_BINDING, bindingFrameBuffer, 0);

        // ?????????????????????RenderBuffer
        glGetIntegerv(GL_RENDERBUFFER_BINDING, bindingRenderBuffer, 0);

        // ??????viewpoint
        glGetIntegerv(GL_VIEWPORT, viewPoint, 0);

        // ??????????????????
        vertexAttribEnableArray.clear();
        for (int x = 0 ; x < vertexAttribEnableArraySize; x++) {
            int[] vertexAttribEnable = new int[1];
            glGetVertexAttribiv(x, GL_VERTEX_ATTRIB_ARRAY_ENABLED, vertexAttribEnable, 0);
            if (vertexAttribEnable[0] != 0) {
                vertexAttribEnableArray.add(x);
            }
        }
    }

    private void restoreOpenGLState() {
        // ?????????????????????FrameBuffer
        glBindFramebuffer(GL_FRAMEBUFFER, bindingFrameBuffer[0]);

        // ?????????????????????RenderBuffer
        glBindRenderbuffer(GL_RENDERBUFFER, bindingRenderBuffer[0]);

        // ??????viewpoint
        glViewport(viewPoint[0], viewPoint[1], viewPoint[2], viewPoint[3]);

        // ??????????????????
        for (int x = 0 ; x < vertexAttribEnableArray.size(); x++) {
            glEnableVertexAttribArray(vertexAttribEnableArray.get(x));
        }
    }

    /**
     * ????????????Texture
     */
    public void addCacheTexture(final MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel textureModel) {
        textureTransitionInput.addCacheTexture(textureModel);
    }

    /**
     * ????????????Texture
     */
    public void removeCacheTexture(final MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel textureModel) {
        textureTransitionInput.removeCacheTexture(textureModel);
    }

    /**
     * ????????????
     */
    public void clearCache() {
        textureTransitionInput.clearCache();
    }

    /**
     * ????????????Texture
     */
    public List<MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel> getCacheTextures() {
        return textureTransitionInput.getCacheTextures();
    }

    /**
     * ????????????Texture
     */
    public void setRenderTextures(List<MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel> renderTextures) {
        textureTransitionInput.setRenderTextures(renderTextures);
    }

    /**
     * ????????????Texture
     */
    public List<MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel> getRenderTextures() {
        return textureTransitionInput.getRenderTextures();
    }

    public void destroy() {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                if (outputTexture[0] != 0) {
                    glDeleteTextures(1, outputTexture, 0);
                    outputTexture[0] = 0;
                }
            }
        });

        textureTransitionInput.destroy();
        textureOutput.destroy();
        commonInputFilter.destroy();
        commonOutputFilter.destroy();
    }

}