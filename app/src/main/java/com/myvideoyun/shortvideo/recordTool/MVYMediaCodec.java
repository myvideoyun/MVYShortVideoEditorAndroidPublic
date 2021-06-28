package com.myvideoyun.shortvideo.recordTool;

import android.graphics.PointF;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaMuxer;
import android.opengl.GLES20;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.myvideoyun.shortvideo.GPUImage.MVYGLProgram;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFilter;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageFramebuffer;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static android.opengl.GLES20.*;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFit;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.TAG;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.floatArrayToBuffer;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.getAspectRatioInsideSize;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.noRotationTextureCoordinates;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants.rotateRightTextureCoordinates;
import static com.myvideoyun.shortvideo.GPUImage.MVYGPUImageEGLContext.syncRunOnRenderThread;

/**
 * Created by 汪洋 on 2019/2/11.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 *
 * MediaCodec相关代码参考了Google Codec Sample
 * https://android.googlesource.com/platform/cts/+/kitkat-release/tests/tests/media/src/android/media/cts/MediaCodecTest.java
 *
 */
public class MVYMediaCodec {

    // ----- GLES 相关变量 -----
    private MVYGPUImageEGLContext eglContext;

    private int boundingWidth;
    private int boundingHeight;

    private MVYGLProgram filterProgram;
    private MVYGPUImageFramebuffer outputFramebuffer;

    private int filterPositionAttribute, filterTextureCoordinateAttribute;
    private int filterInputTextureUniform;

    private Buffer imageVertices = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.imageVertices);
    private Buffer textureCoordinates = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.noRotationTextureCoordinates);

    private MVYGPUImageConstants.MVYGPUImageContentMode contentMode = MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFit;

    // ----- MediaCodec 相关变量 -----

    // 编码开始时间
    private long startTime = 0;

    // 编码器
    private MediaCodec videoEncoder;
    private MediaCodec audioEncoder;
    private static final int TIMEOUT = 1000;

    // 音视频合成器
    private AYMp4Muxer mp4Muxer;

    // 视频编码完成时用到的锁
    private Boolean isRecordFinish = false;
    private ReadWriteLock recordFinishLock = new ReentrantReadWriteLock(true);

    // ----- MediaCodec 时间戳相关变量 -----
    private float videoSpeedRate = 1;

    public MVYMediaCodec(String path, int trackCount) {
        // 创建音视频合成器
        mp4Muxer = new AYMp4Muxer(trackCount);
        try {
            mp4Muxer.setPath(path);
        } catch (IOException e) {
            Log.d(MVYGPUImageConstants.TAG, "文件保存路径无法访问");
            e.printStackTrace();
        }
    }

    /**
     * 设置窗口缩放方式
     */
    public void setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode contentMode) {
        this.contentMode = contentMode;
    }

    /**
     * 配置和启用视频编码器
     */
    public boolean configureVideoCodecAndStart(final int width, final int height, int bitrate, int fps, int iFrameInterval, float speedRate) {
        if (width % 16 != 0 && height % 16 != 0) {
            Log.w(MVYGPUImageConstants.TAG, "width = " + width + " height = " + height + " Compatibility is not good");
        }

        // 配置视频媒体格式
        final MediaFormat format = MediaFormat.createVideoFormat(MVYMediaCodecHelper.MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);

        // 配置时间戳速度
        videoSpeedRate = speedRate;

        // 创建MediaCodec硬编码器
        boolean hadError = false;

        try {
            videoEncoder = MediaCodec.createEncoderByType(MVYMediaCodecHelper.MIME_TYPE);
            videoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        }catch (Throwable e) {
            Log.w(MVYGPUImageConstants.TAG, "video mediaCodec create error: " + e);
            hadError = true;
        } finally {
            if (videoEncoder != null && hadError) {
                videoEncoder.stop();
                videoEncoder.release();
                videoEncoder = null;
            }
        }

        if (hadError) {
            return false;
        }

        // 创建视频编码器数据输入用到的EGL和GLES
        boundingWidth = height; // 交换一下, GL绘制比较方便
        boundingHeight = width;
        initEGLContext();

        videoEncoder.start();

        Log.d(MVYGPUImageConstants.TAG, "video mediaCodec create success");

        // 开启编码线程
        new Thread(){
            @Override
            public void run() {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                int trackIndex = -1;
                long presentationTimeUs = -1;

                for (;;) {
                    recordFinishLock.readLock().lock();

                    if (isRecordFinish) {
                        Log.i(MVYGPUImageConstants.TAG, "视频编码器输出完成");
                        recordFinishLock.readLock().unlock();
                        return;
                    }

                    // 初始化合成器成功, 等待写入数据
                    if (trackIndex >= 0) {
                        if (!mp4Muxer.canWriteData()) {
                            Log.i(MVYGPUImageConstants.TAG, "视频编码器初始化完成, 等待写入数据");
                            recordFinishLock.readLock().unlock();
                            SystemClock.sleep(1);
                            continue;
                        }
                    }

                    int index = videoEncoder.dequeueOutputBuffer(info, TIMEOUT);

                    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat format = videoEncoder.getOutputFormat();
                        Log.d(MVYGPUImageConstants.TAG, "视频编码器初始化完成");

                        // 添加视频轨道信息到合成器
                        trackIndex = mp4Muxer.addTrack(format);

                    }else if (index >= 0) {
                        // 添加视频数据到合成器
                        ByteBuffer byteBuffer;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            byteBuffer = videoEncoder.getOutputBuffer(index);
                        } else {
                            byteBuffer = videoEncoder.getOutputBuffers()[index];
                        }

                        if (info.presentationTimeUs > presentationTimeUs || info.presentationTimeUs == 0) {
                            mp4Muxer.addData(trackIndex, byteBuffer, info);
                            presentationTimeUs = info.presentationTimeUs;
                        }

                        videoEncoder.releaseOutputBuffer(index, false);

                        // 最后一个输出
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.i(MVYGPUImageConstants.TAG, "视频编码器输出完成");
                            recordFinishLock.readLock().unlock();
                            return;
                        }
                    }

                    recordFinishLock.readLock().unlock();
                }
            }
        }.start();

        return true;
    }

    /**
     * 配置和启用音频编码器
     */
    public boolean configureAudioCodecAndStart(int bitrate, int sampleRate) {
        MediaFormat format = MediaFormat.createAudioFormat(MVYMediaCodecHelper.MIME_TYPE_AUDIO, sampleRate, 1);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, CodecProfileLevel.AACObjectLC); // 最广泛支持的AAC配置
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);

        boolean hadError = false;
        try {
            audioEncoder = MediaCodec.createEncoderByType(MVYMediaCodecHelper.MIME_TYPE_AUDIO);
            audioEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Throwable e) {
            Log.w(MVYGPUImageConstants.TAG, "audio mediaCodec create error: " + e);
            hadError = true;
        } finally {
            if (audioEncoder != null && hadError) {
                audioEncoder.stop();
                audioEncoder.release();
                audioEncoder = null;
            }
        }

        if (hadError) {
            return false;
        }

        audioEncoder.start();

        Log.d(MVYGPUImageConstants.TAG, "audio mediaCodec create success");

        // 开启编码线程
        new Thread() {
            @Override
            public void run() {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                int trackIndex = -1;
                long presentationTimeUs = -1;

                for (;;) {
                    recordFinishLock.readLock().lock();

                    if (isRecordFinish) {
                        Log.i(MVYGPUImageConstants.TAG, "音频编码器输出完成");
                        recordFinishLock.readLock().unlock();
                        return;
                    }

                    // 初始化合成器成功, 等待写入数据
                    if (trackIndex >= 0) {
                        if (!mp4Muxer.canWriteData()) {
                            Log.i(MVYGPUImageConstants.TAG, "音频编码器初始化完成, 等待写入数据");
                            recordFinishLock.readLock().unlock();
                            SystemClock.sleep(1);
                            continue;
                        }
                    }

                    // 从编码器中取出一个输出buffer
                    int index = audioEncoder.dequeueOutputBuffer(info, TIMEOUT);

                    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat format = audioEncoder.getOutputFormat();
                        Log.d(MVYGPUImageConstants.TAG, "音频编码器初始化完成");

                        // 添加音频轨道信息到合成器
                        trackIndex = mp4Muxer.addTrack(format);

                    }else if (index >= 0) {
                        // 添加视频数据到合成器
                        ByteBuffer byteBuffer;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            byteBuffer = audioEncoder.getOutputBuffer(index);
                        }else{
                            byteBuffer = audioEncoder.getOutputBuffers()[index];
                        }

                        if (info.presentationTimeUs > presentationTimeUs /* || info.presentationTimeUs == 0*/) {
                            mp4Muxer.addData(trackIndex, byteBuffer, info);
                            presentationTimeUs = info.presentationTimeUs;
                        }

                        // 返回一个输出buffer到编码器中
                        audioEncoder.releaseOutputBuffer(index, false);

                        // 最后一个输出
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.i(MVYGPUImageConstants.TAG, "音频编码器输出完成");
                            recordFinishLock.readLock().unlock();
                            return;
                        }
                    }

                    recordFinishLock.readLock().unlock();
                }
            }
        }.start();

        return true;
    }

    private void initEGLContext() {
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                eglContext = new MVYGPUImageEGLContext();
                eglContext.initEGLWindow(videoEncoder.createInputSurface());

                filterProgram = new MVYGLProgram(MVYGPUImageFilter.kMVYGPUImageVertexShaderString, MVYGPUImageFilter.kMVYGPUImagePassthroughFragmentShaderString);
                filterProgram.link();

                filterPositionAttribute = filterProgram.attributeIndex("position");
                filterTextureCoordinateAttribute = filterProgram.attributeIndex("inputTextureCoordinate");
                filterInputTextureUniform = filterProgram.uniformIndex("inputImageTexture");
            }
        });
    }

    /**
     * 写入视频数据
     */
    public void writeImageTexture(final int texture, final int width, final int height, final long timeStamp) {
        // 设置视频写入的时间
        if (startTime == 0) {
            startTime = timeStamp;
        }
        final long time = (long) ((timeStamp - startTime) * (1/videoSpeedRate));

        recordFinishLock.readLock().lock();

        if (isRecordFinish) {
            recordFinishLock.readLock().unlock();
            return;
        }

        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                eglContext.makeCurrent();

                eglContext.setTimeStemp(time);

                filterProgram.use();

                if (outputFramebuffer != null) {
                    if (boundingWidth != outputFramebuffer.width || boundingHeight != outputFramebuffer.height) {
                        outputFramebuffer.destroy();
                        outputFramebuffer = null;
                    }
                }

                if (outputFramebuffer == null) {
                    outputFramebuffer = new MVYGPUImageFramebuffer(boundingWidth, boundingHeight);
                }

                outputFramebuffer.activateFramebuffer();

                glViewport(0, 0, boundingWidth, boundingHeight);

                glClearColor(0, 0, 0, 0);
                glClear(GL_COLOR_BUFFER_BIT);

                glActiveTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_2D, texture);

                glUniform1i(filterInputTextureUniform, 2);

                PointF insetSize = MVYGPUImageConstants.getAspectRatioInsideSize(new PointF(width, height), new PointF(boundingWidth, boundingHeight));

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

                glEnableVertexAttribArray(filterPositionAttribute);
                glEnableVertexAttribArray(filterTextureCoordinateAttribute);

                GLES20.glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, MVYGPUImageConstants.floatArrayToBuffer(squareVertices));
                glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, textureCoordinates);

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                glBindFramebuffer(GL_FRAMEBUFFER, 0);

                glViewport(0, 0, boundingHeight, boundingWidth);

                glClearColor(0, 0, 0, 0);
                glClear(GL_COLOR_BUFFER_BIT);

                glActiveTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_2D, outputFramebuffer.texture[0]);

                glUniform1i(filterInputTextureUniform, 2);

                Buffer textureCoordinates = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.rotateRightTextureCoordinates);

                glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, imageVertices);
                glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, textureCoordinates);

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                glDisableVertexAttribArray(filterPositionAttribute);
                glDisableVertexAttribArray(filterTextureCoordinateAttribute);

                eglContext.swapBuffers();
            }
        });

        recordFinishLock.readLock().unlock();
    }

    /**
     * 写入音频数据
     */
    public void writePCMByteBuffer(ByteBuffer source, final long timeStamp) {
        // 设置音频写入的时间
        if (startTime == 0) {
            startTime = timeStamp;
        }
        long time = timeStamp - startTime;

        recordFinishLock.readLock().lock();

        if (isRecordFinish) {
            recordFinishLock.readLock().unlock();
            return;
        }

        short[] shorts = new short[source.limit()/2];
        source.position(0);
        source.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

        // 编码
        int inputIndex = audioEncoder.dequeueInputBuffer(TIMEOUT);
        while (inputIndex == -1) {
            inputIndex = audioEncoder.dequeueInputBuffer(TIMEOUT);
        }

        ByteBuffer inputBuffer;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            inputBuffer = audioEncoder.getInputBuffer(inputIndex);
        }else{
            inputBuffer = audioEncoder.getInputBuffers()[inputIndex];
        }

        inputBuffer.clear();
        inputBuffer.limit(source.limit());
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);

        audioEncoder.queueInputBuffer(inputIndex, 0, inputBuffer.limit(), time, 0);

        recordFinishLock.readLock().unlock();
    }

    /**
     * 完成音视频录制
     */
    public void finish() {
        // 等待MediaCodec读锁释放
        Log.d(MVYGPUImageConstants.TAG, "recordFinishLock lock");
        recordFinishLock.writeLock().lock();
        isRecordFinish = true;
        recordFinishLock.writeLock().unlock();
        Log.d(MVYGPUImageConstants.TAG, "recordFinishLock unlock");

        // 释放MediaCodec
        Log.d(MVYGPUImageConstants.TAG, "释放MediaCodec");
        if (videoEncoder != null) {
            videoEncoder.stop();
            videoEncoder.release();
        }

        if (audioEncoder != null) {
            audioEncoder.stop();
            audioEncoder.release();
        }

        // 等待合成器结束
        Log.d(MVYGPUImageConstants.TAG, "释放合成器");
        mp4Muxer.finish();

        // 释放GLES
        Log.d(MVYGPUImageConstants.TAG, "释放GLES");
        MVYGPUImageEGLContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                if (filterProgram != null) {
                    filterProgram.destroy();
                }

                if (outputFramebuffer != null) {
                    outputFramebuffer.destroy();
                }

                if (eglContext != null) {
                    eglContext.destroyEGLWindow();
                    eglContext = null;
                }
            }
        });

        Log.d(MVYGPUImageConstants.TAG, "释放完成");
    }

    private static class AYMp4Muxer {

        private MediaMuxer muxer;
        private int trackCount = 0;
        private int maxTrackCount = 2;
        private ReadWriteLock lock = new ReentrantReadWriteLock(false);

        private AYMp4Muxer(){}

        public AYMp4Muxer(int maxTrackCount) {
            this.maxTrackCount = maxTrackCount;
        }

        /**
         * 设置路径
         */
        void setPath(String path) throws IOException {
            trackCount = 0;
            muxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            muxer.setOrientationHint(90);
        }

        /**
         * 设置音视频轨道
         */
        int addTrack(MediaFormat mediaFormat){
            lock.writeLock().lock();

            if (muxer == null) {
                lock.writeLock().unlock();
                return -1;
            }

            int trackIndex = muxer.addTrack(mediaFormat);
            trackCount++;

            if (trackCount == maxTrackCount) {
                muxer.start();
                Log.d(MVYGPUImageConstants.TAG, "开始muxer");
            }

            lock.writeLock().unlock();
            return trackIndex;
        }

        boolean canWriteData() {
            boolean result = false;

            lock.readLock().lock();

            if (trackCount == maxTrackCount) {
                result = true;
            }

            lock.readLock().unlock();

            return result;
        }

        /**
         * 写入数据
         */
        void addData(int trackIndex, ByteBuffer buffer, MediaCodec.BufferInfo info) {
            lock.readLock().lock();

            if (muxer == null) {
                lock.readLock().unlock();
                return;
            }

            if (trackIndex == -1) {
                lock.readLock().unlock();
                return;
            }

            if (info.size == 0) {
                lock.readLock().unlock();
                return;
            }

            if (trackCount == maxTrackCount) {

                buffer.position(info.offset);
                buffer.limit(info.offset+ info.size);

                muxer.writeSampleData(trackIndex, buffer, info);
            }

            lock.readLock().unlock();
        }

        /**
         * 写入完成
         */
        void finish() {
            lock.writeLock().lock();

            if (muxer == null) {
                lock.writeLock().unlock();
                return;
            }
            try {
                muxer.stop();
                muxer.release();
            }catch (IllegalStateException e) {
                Log.d(MVYGPUImageConstants.TAG, "AYMediaMuxer 关闭失败");
                e.printStackTrace();
            } finally {
                muxer = null;
                lock.writeLock().unlock();
            }
        }
    }
}
