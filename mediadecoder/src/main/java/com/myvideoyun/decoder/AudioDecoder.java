package com.myvideoyun.decoder;

import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AudioDecoder {

    private static final String TAG = "AudioDecoder";

    static {
        System.loadLibrary("mediadecoder");
        System.loadLibrary("ijkffmpeg");
    }


    // 解码监听
    private AudioDecoderListener decoderListener;

    // 播放监听
    private AudioPlaybackListener playbackListener;

    private boolean usePlayer;

    // 解码数据队列
    private LinkedBlockingQueue<AudioFrame> framesQueue;

    // 解码器
    private List<Integer> ffAudioDecoders;

    // 解码锁
    private boolean isDecodeStop = false;
    private ReadWriteLock decodeLock = new ReentrantReadWriteLock(true);

    // 播放锁
    private boolean isPlaybackStop = false;
    private ReadWriteLock playbackLock = new ReentrantReadWriteLock(true);

    // 解码器seek
    private double seekTime = 0;

    public AudioDecoder(boolean usePlayer) {
        this.usePlayer = usePlayer;

        if (usePlayer) {
            framesQueue = new LinkedBlockingQueue<>(100);
        }
    }

    /**
     * 创建本地解码器
     * @param paths 多个视频路径地址
     */
    public void createNativeAudioDecoder(final String[] paths) {
        registerFFmpeg();

        ffAudioDecoders = new ArrayList<>(paths.length);

        for (String path : paths) {
            // 创建本地解码器
            int ffAudioDecoder = initAudioDecoder();
            ffAudioDecoders.add(ffAudioDecoder);

            // 打开视频文件
            String fileName = path.substring(path.lastIndexOf("/"));
            openFile(ffAudioDecoder, path, fileName);
        }
    }

    /**
     * 销毁本地解码器
     */
    public void destroyNativeAudioDecoder() {
        // 加锁
        decodeLock.writeLock().lock();

        if (!isDecodeStop) {
            isDecodeStop = true;

            for (int ffAudioDecoder : ffAudioDecoders) {
                if (ffAudioDecoder != 0) {
                    closeFile(ffAudioDecoder);
                    deinitAudioDecoder(ffAudioDecoder);
                }
            }
            ffAudioDecoders.clear();
        }

        // 解锁
        decodeLock.writeLock().unlock();
    }

    /**
     * 从第一帧开始解码
     */
    public void startDecodeFromFirstFrame() {
        startDecodeFromFirstFrame(0);
    }
    /**
     * 从第一帧开始解码
     */
    public void startDecodeFromFirstFrame(final double seekTime) {

        stopDecoder();

        isDecodeStop = false;

        this.seekTime = seekTime;

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "从第一帧开始解码");

                // 计算每个单频的长度和总长度
                double totalAudioLength = 0;
                double[] audioLengths = new double[ffAudioDecoders.size()];
                for (int i = 0; i < ffAudioDecoders.size(); i++) {
                    audioLengths[i] = getAudioLength(ffAudioDecoders.get(i));
                    totalAudioLength += audioLengths[i];
                }

                for (int i = 0; i < ffAudioDecoders.size(); i ++) {

                    // 加锁
                    decodeLock.readLock().lock();

                    if (isDecodeStop) {
                        decodeLock.readLock().unlock();

                        try {
                            if (framesQueue != null) {
                                AudioFrame frame = new AudioFrame();
                                frame.flag = -2;
                                framesQueue.put(frame);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (decoderListener != null) {
                            decoderListener.decoderStop(AudioDecoder.this);
                        }

                        return;
                    }

                    // 获取解码器
                    int ffAudioDecoder = ffAudioDecoders.get(i);

                    // 从第一帧开始
                    if (ffAudioDecoder != 0) {

                        // 算出当前解码器最后一帧全局pts
                        double firstFrameGlobalPts = 0;
                        for (int x = 0; x < i; x++) {
                            firstFrameGlobalPts += audioLengths[x];
                        }

                        // 算出当前解码器最后一帧全局pts
                        double lastFrameGlobalPts = 0;
                        for (int x = 0; x <= i; x++) {
                            lastFrameGlobalPts += audioLengths[x];
                        }

                        if (seekTime >= lastFrameGlobalPts) { // seek的时间超过了当前解码器的时长
                            // 进入下一个解码器
                            decodeLock.readLock().unlock();
                            continue;

                        } else if (seekTime >= firstFrameGlobalPts && seekTime < lastFrameGlobalPts) { // seek的时间在当前解码器的范围内

                            // seek到 seek时间点的前一个I帧
                            if (seekTime - firstFrameGlobalPts == 0) {
                                forwardSeekTo(ffAudioDecoder, 0);
                                Log.e(TAG, "第 " + (i+1) + " 个解码器, 开始解码");

                            } else {
                                backwardSeekTo(ffAudioDecoder, seekTime - firstFrameGlobalPts);
                                Log.e(TAG, "第 " + (i+1) + " 个解码器, 开始解码");

                            }

                            // 解码到seek时间点
                            out: while (true) {

                                // 解码数据
                                AudioFrame[] frames = decodeAFrame(ffAudioDecoder);

                                for (final AudioFrame frame : frames) {

                                    // 设置全局 pts 和 length 数据
                                    frame.globalPts = frame.pts;
                                    for (int x = 0; x < i; x++) {
                                        frame.globalPts += audioLengths[x];
                                    }
                                    frame.globalLength = totalAudioLength;

                                    // 解码到seek点
                                    if (frame.globalPts >= seekTime || Math.abs(frame.globalPts - seekTime) < 10) {
                                        Log.e(TAG, "seek处理完成");
                                        break out;
                                    }
                                }

                                SystemClock.sleep(1);
                            }

                        } else {
                            // 不需要处理seek, 正常解码
                            forwardSeekTo(ffAudioDecoder, 0);
                            Log.e(TAG, "第 " + (i+1) + " 个解码器, 开始解码");
                        }

                    } else {
                        decodeLock.readLock().unlock();

                        try {
                            if (framesQueue != null) {
                                AudioFrame frame = new AudioFrame();
                                frame.flag = -2;
                                framesQueue.put(frame);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (decoderListener != null) {
                            decoderListener.decoderStop(AudioDecoder.this);
                        }
                        return;
                    }

                    // 解锁
                    decodeLock.readLock().unlock();

                    out: while (true) {
                        // 加锁
                        decodeLock.readLock().lock();

                        if (isDecodeStop) {
                            decodeLock.readLock().unlock();

                            try {
                                if (framesQueue != null) {
                                    AudioFrame frame = new AudioFrame();
                                    frame.flag = -2;
                                    framesQueue.put(frame);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            if (decoderListener != null) {
                                decoderListener.decoderStop(AudioDecoder.this);
                            }
                            return;
                        }

                        // 解码数据
                        AudioFrame[] frames = decodeAFrame(ffAudioDecoder);

                        for (final AudioFrame frame : frames) {

                            // 设置全局 pts 和 length 数据
                            frame.globalPts = frame.pts;
                            for (int x = 0; x < i; x++) {
                                frame.globalPts += audioLengths[x];
                            }
                            frame.globalLength = totalAudioLength;

                            try {
                                if (decoderListener != null) {

                                    // 添加解码数据到队列中
                                    List<AudioFrame> audioFrames = decoderListener.decoderAudioOutput(AudioDecoder.this, frame);

                                    if (framesQueue != null) {
                                        for (AudioFrame audioFrame : audioFrames) {
                                            framesQueue.put(audioFrame);
                                        }
                                    }
                                } else {
                                    if (framesQueue != null) {
                                        framesQueue.put(frame);
                                    }
                                }

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            // 是否到结尾
                            if (Math.abs(frame.pts + frame.duration - frame.length) < 10 || frame.pts + frame.duration > frame.length) {

                                decodeLock.readLock().unlock();

                                // 解码到最后一帧
                                Log.e(TAG, "解码到eof");
                                break out;
                            }
                        }

                        // 解锁
                        decodeLock.readLock().unlock();
                        SystemClock.sleep(1);
                    }
                }

                try {
                    if (framesQueue != null) {
                        AudioFrame frame = new AudioFrame();
                        frame.flag = -1;
                        framesQueue.put(frame);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 解码完成
                isDecodeStop = true;

                if (decoderListener != null) {
                    decoderListener.decoderFinish(AudioDecoder.this);
                }
            }
        }).start();
    }

    /**
     * 停止解码器
     */
    public void stopDecoder() {
        // 加锁
        decodeLock.writeLock().lock();

        isDecodeStop = true;

        // 解锁
        decodeLock.writeLock().unlock();
    }

    /**
     * 开始播放
     */
    public void startPlayer() {

        // 关闭上一次的播放器
        stopPlayer();

        // 重置新播放器的状态
        isPlaybackStop = false;

        new Thread(new Runnable() {
            @Override
            public void run () {

                while (true) {
                    try {
                        // 加锁
                        playbackLock.readLock().lock();
                        if (isPlaybackStop) {
                            playbackLock.readLock().unlock();

                            if (playbackListener != null) {
                                playbackListener.playbackStop(AudioDecoder.this);
                            }

                            return;
                        }

                        AudioFrame frame = framesQueue.take();

                        // 解码到最后一帧
                        if (frame.flag < 0) {
                            playbackLock.readLock().unlock();

                            isPlaybackStop = true;

                            if (playbackListener != null) {
                                playbackListener.playbackFinish(AudioDecoder.this);
                            }
                            return;
                        }

                        if (playbackListener != null) {
                            playbackListener.playbackAudioOutput(AudioDecoder.this, frame);
                        }

                        playbackLock.readLock().unlock();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 停止播放
     */
    public void stopPlayer() {
        // 加锁
        playbackLock.writeLock().lock();

        // 如果播放器已经关闭, 清空不需要的数据
        if (framesQueue != null) framesQueue.clear();

        isPlaybackStop = true;

        // 解锁
        playbackLock.writeLock().unlock();
    }

    public void setDecoderListener(AudioDecoderListener decoderListener) {
        this.decoderListener = decoderListener;
    }

    public void setPlaybackListener(AudioPlaybackListener playbackListener) {
        this.playbackListener = playbackListener;
    }

    /**
     * 解码监听
     */
    public interface AudioDecoderListener {

        /**
         * 返回解码成功的帧数据
         */
        List<AudioFrame> decoderAudioOutput(AudioDecoder decoder, AudioFrame audioFrame);

        /**
         * 解码停止
         */
        void decoderStop(AudioDecoder decoder);

        /**
         * 解码完成
         */
        void decoderFinish(AudioDecoder decoder);
    }

    /**
     * 播放监听
     */
    public interface AudioPlaybackListener  {

        /**
         * 返回需要渲染的帧数据
         */
        void playbackAudioOutput(AudioDecoder decoder, AudioFrame audioFrame);

        /**
         * 播放停止完成
         */
        void playbackStop(AudioDecoder decoder);

        /**
         * 播放结束
         */
        void playbackFinish(AudioDecoder decoder);
    }

    private native void registerFFmpeg();

    private native int initAudioDecoder();

    private native void deinitAudioDecoder(int instance);

    private native boolean openFile(int instance, String path, String fileName);

    private native AudioFrame[] decodeAFrame(int instance);

    private native void forwardSeekTo(int instance, double millisecond);

    private native void backwardSeekTo(int instance, double millisecond);

    private native double getAudioLength(int instance);

    private native void closeFile(int instance);
}
