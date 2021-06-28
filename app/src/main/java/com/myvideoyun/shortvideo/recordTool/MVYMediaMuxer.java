package com.myvideoyun.shortvideo.recordTool;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by yangshunfa on 2019/3/12.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class MVYMediaMuxer {

    public static final String TAG = "MVYMediaMuxer";
    // 原生视频路径
//    private final String mediaPath;
    private final String outputVideoPath;
    private final String outputAudioPath;
    private MediaExtractor extractor;
    private int bufferSize = 100 * 1024;
    private MediaMuxer muxer;
    private MVYMediaMuxer.ProgressListener listener;
    private int numTracks;

    public MVYMediaMuxer(String outputVideoPath, String outputAudioPath) {
//        this.mediaPath = mediapath;
        this.outputVideoPath = outputVideoPath;
        this.outputAudioPath = outputAudioPath;
        extractor = new MediaExtractor();
        muxer = null;
        try {
//            muxer = new MediaMuxer("temp.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            muxer = new MediaMuxer(outputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void muxingAudioAndVideo(final String sourcePath){
        new Thread(){
            @Override
            public void run() {
                try {
//                    String outputPath = MVYApplication.instance.getBaseContext().getExternalCacheDir()
//                            + "/" + UUID.randomUUID().toString().replace("-", "") + ".mp4";
//                    String audioOutputPath = MVYApplication.instance.getBaseContext().getExternalCacheDir()
//                            + "/" + UUID.randomUUID().toString().replace("-", "") + ".aac";

                    MediaMuxer mMediaMuxer = new MediaMuxer(outputAudioPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//                    MediaMuxer mVideoMuxer = new MediaMuxer(outputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

//                    // 视频的MediaExtractor
//                    MediaExtractor mVideoExtractor = new MediaExtractor();
//                    mVideoExtractor.setDataSource(sourcePath);
//                    int videoTrackIndex = -1;
//                    for (int i = 0; i < mVideoExtractor.getTrackCount(); i++) {
//                        MediaFormat format = mVideoExtractor.getTrackFormat(i);
//                        if (format.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
//                            mVideoExtractor.selectTrack(i);
//                            videoTrackIndex = mVideoMuxer.addTrack(format);
//                            break;
//                        }
//                    }

                    // 音频的MediaExtractor
                    MediaExtractor mAudioExtractor = new MediaExtractor();
                    mAudioExtractor.setDataSource(sourcePath);
                    int audioTrackIndex = -1;
                    for (int i = 0; i < mAudioExtractor.getTrackCount(); i++) {
                        MediaFormat format = mAudioExtractor.getTrackFormat(i);
                        if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                            mAudioExtractor.selectTrack(i);
                            audioTrackIndex = mMediaMuxer.addTrack(format);
                            break;
                        }
                    }

                    // 添加完所有轨道后start
                    mMediaMuxer.start();
//                    mVideoMuxer.start();
//
//                    // 封装视频track
//                    if (-1 != videoTrackIndex) {
//                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//                        info.presentationTimeUs = 0;
//                        ByteBuffer buffer = ByteBuffer.allocate(100 * 1024);
//                        while (true) {
//                            int sampleSize = mVideoExtractor.readSampleData(buffer, 0);
//                            if (sampleSize < 0) {
//                                break;
//                            }
//                            info.offset = 0;
//                            info.size = sampleSize;
//                            info.flags = mVideoExtractor.getSampleFlags();
//                            info.presentationTimeUs = mVideoExtractor.getSampleTime();
//                            mVideoMuxer.writeSampleData(videoTrackIndex, buffer, info);
//
//                            mVideoExtractor.advance();
//                        }
//                    }

                    int size = 0;
                    // 封装音频track
                    if (-1 != audioTrackIndex) {
                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                        info.presentationTimeUs = 0;
                        ByteBuffer buffer = ByteBuffer.allocate(100 * 1024);
                        while (true) {
                            int sampleSize = mAudioExtractor.readSampleData(buffer, 0);
                            if (sampleSize < 0) {
                                break;
                            }
                            info.offset = 0;
                            info.size = sampleSize;
                            size += sampleSize;
                            info.flags = mAudioExtractor.getSampleFlags();
                            info.presentationTimeUs = mAudioExtractor.getSampleTime();
                            mMediaMuxer.writeSampleData(audioTrackIndex, buffer, info);

                            mAudioExtractor.advance();
                        }
                        Log.e("moose", "视频总长度：" + size);
                    }

                    // 释放MediaExtractor
//                    mVideoExtractor.release();
                    mAudioExtractor.release();

                    // 释放MediaMuxer
                    mMediaMuxer.stop();
                    mMediaMuxer.release();
//                    // 释放MediaMuxer
//                    mVideoMuxer.stop();
//                    mVideoMuxer.release();
                    if (listener != null){
                        listener.onAudioSucceed(outputAudioPath);
//                        listener.onVideoSucceed(outputVideoPath);
                    }
                    Log.e(TAG, "完成");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "分离音视频出错了。");
                }
            }
        }.start();
    }

    public void setListener(ProgressListener l) {
        this.listener = l;
    }

    public interface ProgressListener {

        void onProgress(float present);

        void onFailure(String message);

        void onVideoSucceed(String path);

        void onAudioSucceed(String path);
    }
}
