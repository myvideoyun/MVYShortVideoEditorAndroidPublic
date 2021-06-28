package com.myvideoyun.decoder;

import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class VideoDecoder{

    private static final String TAG = "VideoDecoder";

    static {
        System.loadLibrary("mediadecoder");
        System.loadLibrary("ijkffmpeg");
    }

    // 解码监听
    private VideoDecoderListener decoderListener;

    // 播放监听
    private VideoPlaybackListener playbackListener;

    private boolean usePlayer;

    // 解码数据队列
    private LinkedBlockingQueue<VideoFrame> framesQueue;

    // 解码器
    private List<Integer> ffVideoDecoders;

    // 解码锁
    private boolean isDecodeStop = false;
    private ReadWriteLock decodeLock = new ReentrantReadWriteLock(true);

    // 播放锁
    private boolean isPlaybackStop = false;
    private ReadWriteLock playbackLock = new ReentrantReadWriteLock(true);

    // 解码器seek
    private double seekTime = 0;

    // 播放器第一帧的时间
    private long playbackFirstFrameTime = 0;

    public VideoDecoder(boolean usePlayer) {
        this.usePlayer = usePlayer;

        if (usePlayer) {
            framesQueue = new LinkedBlockingQueue<>(60);
        }
    }

    /**
     * 创建本地解码器
     * @param paths 多个视频路径地址
     */
    public void createNativeVideoDecoder(final String[] paths) {
        registerFFmpeg();

        ffVideoDecoders = new ArrayList<>(paths.length);

        for (String path : paths) {
            // 创建本地解码器
            int ffVideoDecoder = initVideoDecoder();
            ffVideoDecoders.add(ffVideoDecoder);

            // 打开视频文件
            String fileName = path.substring(path.lastIndexOf("/"));
            openFile(ffVideoDecoder, path, fileName);
        }
    }

    /**
     * 销毁本地解码器
     */
    public void destroyNativeVideoDecoder() {
        // 加锁
        decodeLock.writeLock().lock();

        if (!isDecodeStop) {
            isDecodeStop = true;

            for (int ffVideoDecoder : ffVideoDecoders) {
                if (ffVideoDecoder != 0) {
                    closeFile(ffVideoDecoder);
                    deinitVideoDecoder(ffVideoDecoder);
                }
            }
            ffVideoDecoders.clear();
        }

        // 解锁
        decodeLock.writeLock().unlock();
    }

    /**
     * 从第一帧开始解码
     */
    public void startDecodeFromFirstFrame() {
        startDecodeFromFirstFrame(0, 1.0f);
    }

    public void startFastDecoding(){
        Log.e(TAG, "Fast Play mode");
        startDecodeFromFirstFrame(0, 0.5f);
    }

    public void startSlowDecoding(){
        Log.e(TAG, "Slow Play mode");
        startDecodeFromFirstFrame(0, 2.0f);
    }

    /**
     * 是否播放中
     * @return
     */
    public boolean isPlaying(){
        return !isPlaybackStop;
    }
    /**
     * 是否解码中
     * @return
     */
    public boolean isDecoding(){
        return !isDecodeStop;
    }


    /**
     * 从第一帧开始解码
     */
    public void startDecodeFromFirstFrame(final double seekTime, final float timeScaleFactor) {

        stopDecoder();

        isDecodeStop = false;

        this.seekTime = seekTime;

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "从第一帧开始解码");

                // 计算每个视频的长度和总长度
                double totalVideoLength = 0;
                double[] videoLengths = new double[ffVideoDecoders.size()];
                for (int i = 0; i < ffVideoDecoders.size(); i++) {
                    videoLengths[i] = getVideoLength(ffVideoDecoders.get(i));
                    totalVideoLength += videoLengths[i];
                }
                totalVideoLength *= timeScaleFactor;

                for (int i = 0; i < ffVideoDecoders.size(); i++) {

                    // 加锁
                    decodeLock.readLock().lock();

                    if (isDecodeStop) {
                        decodeLock.readLock().unlock();

                        try {
                            if (framesQueue != null) {
                                VideoFrame frame = new VideoFrame();
                                frame.flag = -2;
                                framesQueue.put(frame);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (decoderListener != null) {
                            decoderListener.decoderStop(VideoDecoder.this);
                        }

                        return;
                    }

                    // 获取解码器
                    int ffVideoDecoder = ffVideoDecoders.get(i);

                    // 从第一帧开始
                    if (ffVideoDecoder != 0) {

                        // 算出当前解码器最后一帧全局pts
                        double firstFrameGlobalPts = 0;
                        for (int x = 0; x < i; x++) {
                            firstFrameGlobalPts += videoLengths[x];
                        }

                        // 算出当前解码器最后一帧全局pts
                        double lastFrameGlobalPts = 0;
                        for (int x = 0; x <= i; x++) {
                            lastFrameGlobalPts += videoLengths[x];
                        }

                        if (seekTime >= lastFrameGlobalPts) { // seek的时间超过了当前解码器的时长
                            // 进入下一个解码器
                            decodeLock.readLock().unlock();
                            continue;

                        } else if (seekTime >= firstFrameGlobalPts && seekTime < lastFrameGlobalPts) { // seek的时间在当前解码器的范围内

                            // seek到 seek时间点的后一个I帧
                            if (seekTime - firstFrameGlobalPts == 0) {
                                backwardSeekTo2(ffVideoDecoder, 1);
                                Log.e(TAG, "第 " + (i+1) + " 个解码器, 开始解码");

                            } else {
                                backwardSeekTo(ffVideoDecoder, seekTime - firstFrameGlobalPts);
                                Log.e(TAG, "第 " + (i+1) + " 个解码器, 开始解码");

                            }

                            // 解码到seek时间点
                            out: while (true) {

                                // 解码数据
                                VideoFrame[] frames = decodeAFrame(ffVideoDecoder);

                                for (final VideoFrame frame : frames) {

                                    // 设置全局 pts 和 length 数据
                                    frame.globalPts = frame.pts;
                                    for (int x = 0; x < i; x++) {
                                        frame.globalPts += videoLengths[x];
                                    }
                                    frame.globalLength = totalVideoLength;

                                    // 解码到seek点
                                    if (frame.globalPts >= seekTime || Math.abs(frame.globalPts - seekTime) < 10) {
                                        Log.e(TAG, "seek处理完成");

                                        try {
                                            if (decoderListener != null) {

                                                // 添加解码数据到队列中
                                                List<VideoFrame> videoFrames = decoderListener.decoderVideoOutput(VideoDecoder.this, frame);

                                                if (framesQueue != null) {
                                                    for (VideoFrame videoFrame : videoFrames) {
                                                        framesQueue.put(videoFrame);
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

                                        break out;
                                    }
                                }

                                SystemClock.sleep(1);
                            }

                        } else {
                            // 不需要处理seek, 正常解码
                            backwardSeekTo2(ffVideoDecoder, 1);
                            Log.e(TAG, "第 " + (i+1) + " 个解码器, 开始解码");
                        }
                    } else {
                        decodeLock.readLock().unlock();

                        try {
                            if (framesQueue != null) {
                                VideoFrame frame = new VideoFrame();
                                frame.flag = -2;
                                framesQueue.put(frame);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (decoderListener != null) {
                            decoderListener.decoderStop(VideoDecoder.this);
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
                                    VideoFrame frame = new VideoFrame();
                                    frame.flag = -2;
                                    framesQueue.put(frame);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            if (decoderListener != null) {
                                decoderListener.decoderStop(VideoDecoder.this);
                            }
                            return;
                        }

                        // 解码数据
                        VideoFrame[] frames = decodeAFrame(ffVideoDecoder);
                        if(frames.length == 0 && isEOS(ffVideoDecoder))
                        {
                            Log.e(TAG, "no more frame in decoder, eof");
                            decodeLock.readLock().unlock();
                            break;
                        }

                        for (final VideoFrame frame : frames) {

                            // 设置全局 pts 和 length 数据
                            frame.pts *= timeScaleFactor;
                            frame.globalPts = frame.pts;
                            for (int x = 0; x < i; x++) {
                                frame.globalPts += videoLengths[x];
                            }
                            frame.globalLength = totalVideoLength;

                            try {
                                if (decoderListener != null) {

                                    // 添加解码数据到队列中
                                    List<VideoFrame> videoFrames = decoderListener.decoderVideoOutput(VideoDecoder.this, frame);

                                    if (framesQueue != null) {
                                        for (VideoFrame videoFrame : videoFrames) {
                                            framesQueue.put(videoFrame);
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
                            if (false && Math.abs(frame.pts + frame.duration - frame.length) < 10 || frame.pts + frame.duration > frame.length) {

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
                        VideoFrame frame = new VideoFrame();
                        frame.flag = -1;
                        framesQueue.put(frame);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 解码完成
                isDecodeStop = true;

                if (decoderListener != null) {
                    decoderListener.decoderFinish(VideoDecoder.this);
                }
            }
        }).start();
    }

    /**
     * 从最后一帧开始解码
     */
    public void startDecodeFromLastFrame() {
        startDecodeFromLastFrame(0);
    }

    /**
     * 从最后一帧开始解码
     */
    public void startDecodeFromLastFrame(final double seekTime) {

        stopDecoder();

        isDecodeStop = false;

        this.seekTime = seekTime;

        new Thread(new Runnable() {
            @Override
            public void run () {
                Log.e(TAG, "从最后一帧开始解码");

                // 计算每个视频的长度和总长度
                double totalVideoLength = 0;
                double[] videoLengths = new double[ffVideoDecoders.size()];
                for (int i = 0; i < ffVideoDecoders.size(); i++) {
                    videoLengths[i] = getVideoLength(ffVideoDecoders.get(i));
                    totalVideoLength += videoLengths[i];
                }

                float iFrameInterval = 100;

                for (int i = ffVideoDecoders.size() - 1; i >= 0; i--) {

                    // 加锁
                    decodeLock.readLock().lock();

                    if (isDecodeStop) {
                        decodeLock.readLock().unlock();

                        try {
                            if (framesQueue != null) {
                                VideoFrame frame = new VideoFrame();
                                frame.flag = -2;
                                framesQueue.put(frame);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (decoderListener != null) {
                            decoderListener.decoderStop(VideoDecoder.this);
                        }
                        return;
                    }

                    // 获取解码器
                    int ffVideoDecoder = ffVideoDecoders.get(i);

                    if (ffVideoDecoder == 0) {
                        decodeLock.readLock().unlock();

                        try {
                            if (framesQueue != null) {
                                VideoFrame frame = new VideoFrame();
                                frame.flag = -2;
                                framesQueue.put(frame);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (decoderListener != null) {
                            decoderListener.decoderStop(VideoDecoder.this);
                        }
                        return;
                    }

                    // 解锁
                    decodeLock.readLock().unlock();

                    // 当前视频帧是否是seek点
                    boolean isSeekPointFrame = false;

                    // 当前视频段是否包含seek时间
                    boolean hasSeekTime = false;

                    // 当前I帧时间
                    double currentIFramePTS = 0;

                    // 下一个I帧时间
                    double nextIFramePTS = 0;

                    List<VideoFrame> gopFrames = new ArrayList<>();

                    Log.e(TAG, "开始解码");

                    // 算出当前解码器第一帧全局pts
                    double firstFrameGlobalPts = 0;
                    for (int x = 0; x < i; x++) {
                        firstFrameGlobalPts += videoLengths[x];
                    }

                    // 算出当前解码器最后一帧全局pts
                    double lastFrameGlobalPts = 0;
                    for (int x = 0; x <= i; x++) {
                        lastFrameGlobalPts += videoLengths[x];
                    }

                    out:
                    while (true) {

                        // 加锁
                        decodeLock.readLock().lock();
                        if (isDecodeStop) {
                            decodeLock.readLock().unlock();

                            try {
                                if (framesQueue != null) {
                                    VideoFrame frame = new VideoFrame();
                                    frame.flag = -2;
                                    framesQueue.put(frame);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            if (decoderListener != null) {
                                decoderListener.decoderStop(VideoDecoder.this);
                            }
                            return;
                        }

                        // 处理当前解码器的开始位置
                        if (currentIFramePTS == 0 && nextIFramePTS == 0) {
                            if (firstFrameGlobalPts >= totalVideoLength - seekTime) { // 当前解码器不需要解码
                                // 解锁
                                decodeLock.readLock().unlock();
                                break;
                            } else if (lastFrameGlobalPts < totalVideoLength - seekTime) { // 当前解码器需要解码, 不需要Seek
                                // 正常从结尾的I帧开始解码
                                backwardSeekTo(ffVideoDecoder, Math.floor(videoLengths[i]));
                                isSeekPointFrame = true;
                            } else { // 在seek范围内, 当前解码器需要解码, 需要Seek
                                // 处理seek
                                double time = totalVideoLength - seekTime - firstFrameGlobalPts;
                                backwardSeekTo(ffVideoDecoder, Math.floor(time));
                                isSeekPointFrame = true;
                                hasSeekTime = true;
                            }
                        }

                        // 视频解码
                        VideoFrame[] frames = decodeAFrame(ffVideoDecoder);

                        if (frames.length == 0) {
                            iFrameInterval += 100;
                        }

                        for (VideoFrame frame : frames) {

                            if (isSeekPointFrame) { // 设置需要解码的视频段信息
                                isSeekPointFrame = false;

                                if (nextIFramePTS == 0 && currentIFramePTS == 0) { // 初始解码的情况
                                    nextIFramePTS = hasSeekTime ? totalVideoLength - seekTime - firstFrameGlobalPts : videoLengths[i];
                                    currentIFramePTS = frame.pts;

                                }else if (frame.pts == currentIFramePTS) { // seek距离太近, 回到了原点
                                    iFrameInterval *= 2;
                                    double time = currentIFramePTS - iFrameInterval;
                                    backwardSeekTo(ffVideoDecoder, time > 0 ? time : 0);
                                    isSeekPointFrame = true;
                                    Log.e(TAG, "seek距离太近, 回到了原点");
                                    break;

                                } else { // 正常seek
                                    nextIFramePTS = currentIFramePTS;
                                    currentIFramePTS = frame.pts;
                                }

                                Log.e(TAG, "nextIFramePTS : " + nextIFramePTS + " currentIFramePTS : " + currentIFramePTS);
                            }

                            gopFrames.add(frame);

                            // 解码到视频段的最后一帧
                            if (frame.pts + frame.duration > nextIFramePTS || Math.abs(frame.pts + frame.duration - nextIFramePTS) < 10) {
                                Log.e(TAG, "解码到视频段的最后一帧");

                                // 倒序排列视频帧, 并添加到视频帧队列
                                for (int x = gopFrames.size() - 1; x >= 0; --x) {

                                    frame = gopFrames.get(x);
                                    // 设置全局 pts 和 length 数据
                                    frame.globalPts = frame.pts;
                                    for (int y = 0; y < i; y++) {
                                        frame.globalPts += videoLengths[y];
                                    }
                                    frame.globalLength = totalVideoLength;
                                    frame.globalPts = frame.globalLength - frame.globalPts - frame.duration;

                                    try {
                                        if (decoderListener != null) {

                                            // 添加解码数据到队列中
                                            List<VideoFrame> videoFrames = decoderListener.decoderVideoOutput(VideoDecoder.this, frame);

                                            if (framesQueue != null) {
                                                for (VideoFrame videoFrame : videoFrames) {
                                                    framesQueue.put(videoFrame);
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
                                }

                                // 重置视频段内容
                                gopFrames = new ArrayList<>();

                                // 判断是否解码完成
                                if (currentIFramePTS > 1) {
                                    double time = currentIFramePTS - iFrameInterval;
                                    Log.e(TAG, "seekTo : " + (time > 0 ? time : 0));
                                    backwardSeekTo(ffVideoDecoder, (time > 0 ? time : 0));
                                    isSeekPointFrame = true;

                                    // 解码下一个视频段
                                    break;
                                } else {
                                    // 解锁
                                    decodeLock.readLock().unlock();

                                    // 解码完成
                                    Log.e(TAG, "解码到eof");
                                    break out;
                                }
                            }
                        }

                        // 解锁
                        decodeLock.readLock().unlock();

                        SystemClock.sleep(1);
                    }
                }

                try {
                    if (framesQueue != null) {
                        VideoFrame frame = new VideoFrame();
                        frame.flag = -1;
                        framesQueue.put(frame);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                isDecodeStop = true;

                if (decoderListener != null) {
                    decoderListener.decoderFinish(VideoDecoder.this);
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

        // 播放器第一帧时间
        playbackFirstFrameTime = 0;

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
                                playbackListener.playbackStop(VideoDecoder.this);
                            }

                            return;
                        }

                        VideoFrame frame = framesQueue.take();

                        // 解码到最后一帧
                        if (frame.flag < 0) {
                            playbackLock.readLock().unlock();

                            isPlaybackStop = true;

                            if (playbackListener != null) {
                                playbackListener.playbackFinish(VideoDecoder.this);
                            }
                            return;
                        }

                        // 解码的是第一个I帧, 等待一会
                        if (frame.isKeyFrame == 1 && Math.abs(frame.globalPts) < 100) {
                            SystemClock.sleep(100);
                        }

                        if (playbackFirstFrameTime == 0) {
                            playbackFirstFrameTime = SystemClock.elapsedRealtime() - (long)seekTime;
                        }

                        // 计算休眠时间
                        long currentTime = SystemClock.elapsedRealtime();

                        while (currentTime - playbackFirstFrameTime < frame.globalPts) {
                            SystemClock.sleep(1);
                            currentTime = SystemClock.elapsedRealtime();
                        }

                        if (playbackListener != null) {
                            playbackListener.playbackVideoOutput(VideoDecoder.this, frame);
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
        framesQueue.clear();

        isPlaybackStop = true;

        // 解锁
        playbackLock.writeLock().unlock();
    }

    public void setDecoderListener(VideoDecoderListener decoderListener) {
        this.decoderListener = decoderListener;
    }

    public void setPlaybackListener(VideoPlaybackListener playbackListener) {
        this.playbackListener = playbackListener;
    }

    public void setPlaybackFirstFrameTime(long playbackFirstFrameTime) {
        playbackLock.writeLock().lock();
        this.playbackFirstFrameTime = playbackFirstFrameTime - (long)seekTime;
        playbackLock.writeLock().unlock();
    }

    public long getPlaybackFirstFrameTime() {
        playbackLock.readLock().lock();
        long returnValue =  playbackFirstFrameTime;
        playbackLock.readLock().unlock();
        return returnValue;
    }

    /**
     * 解码监听
     */
    public interface VideoDecoderListener {

        /**
         * 返回解码成功的帧数据
         */
        List<VideoFrame> decoderVideoOutput(VideoDecoder decoder, VideoFrame videoFrame);

        /**
         * 解码停止
         */
        void decoderStop(VideoDecoder decoder);

        /**
         * 解码完成
         */
        void decoderFinish(VideoDecoder decoder);
    }

    /**
     * 播放监听
     */
    public interface VideoPlaybackListener  {

        /**
         * 返回需要渲染的帧数据
         */
        void playbackVideoOutput(VideoDecoder decoder, VideoFrame videoFrame);

        /**
         * 播放停止完成
         */
        void playbackStop(VideoDecoder decoder);

        /**
         * 播放结束
         */
        void playbackFinish(VideoDecoder decoder);
    }

     native void registerFFmpeg();

     native int initVideoDecoder();

     native void deinitVideoDecoder(int instance);

     native boolean openFile(int instance, String path, String fileName);

     native boolean isEOS(int instance);

     native VideoFrame[] decodeAFrame(int instance);

    native void forwardSeekTo(int instance, double second);

    native void backwardSeekTo2(int instance, int frameIndex);

    native void backwardSeekTo(int instance, double second);

     native double getVideoLength(int instance);

     native void closeFile(int instance);
}
