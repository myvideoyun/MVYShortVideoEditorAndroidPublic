package com.myvideoyun.decoder;

import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class VideoAccurateSeekDecoder {

    private static final String TAG = "SeekDecoder";

    // 视频精准Seek监听
    private VideoAccurateSeekDecoderListener decoderListener;

    // 视频解码器
    private VideoDecoder nativeDecoder = new VideoDecoder(false);

    // 解码器
    private List<Integer> ffVideoDecoders;

    // 解码锁
    private boolean isDecodeStop = false;
    private ReadWriteLock decodeLock = new ReentrantReadWriteLock(true);

    // 帧缓存数组
    private LinkedList<VideoFrame> cacheFrameList = new LinkedList<>();
    private int maxSizeOfCacheFrameList = 60; // 最多缓存60帧

    // 帧预览seek位置
    private double seekTime =  0;
    private boolean isSeekTimeUpdate = false;

    // 视频总长度
    private double totalVideoLength = 0;

    static {
        System.loadLibrary("mediadecoder");
        System.loadLibrary("ijkffmpeg");
    }

    /**
     * 创建本地解码器
     * @param paths 多个视频路径地址
     */
    public void createNativeVideoDecoder(final String[] paths) {
        nativeDecoder.registerFFmpeg();

        ffVideoDecoders = new ArrayList<>(paths.length);

        for (String path : paths) {
            // 创建本地解码器
            int ffVideoDecoder = nativeDecoder.initVideoDecoder();
            ffVideoDecoders.add(ffVideoDecoder);

            // 打开视频文件
            String fileName = path.substring(path.lastIndexOf("/"));
            nativeDecoder.openFile(ffVideoDecoder, path, fileName);
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
                    nativeDecoder.closeFile(ffVideoDecoder);
                    nativeDecoder.deinitVideoDecoder(ffVideoDecoder);
                }
            }
            ffVideoDecoders.clear();
        }

        // 解锁
        decodeLock.writeLock().unlock();
    }

    /**
     * 开始精准Seek解码
     */
    public void startAccurateSeekDecode() {

        stopDecoder();

        isDecodeStop = false;

        new Thread(new Runnable() {
            @Override
            public void run() {

                // 计算每个单频的长度和总长度
                double[] videoLengths = new double[ffVideoDecoders.size()];
                for (int i = 0; i < ffVideoDecoders.size(); i++) {
                    videoLengths[i] = nativeDecoder.getVideoLength(ffVideoDecoders.get(i));
                    totalVideoLength += videoLengths[i];
                }

                while (true) {
                    // 加锁
                    decodeLock.readLock().lock();

                    // 获取解码器
                    int decoderIndex = 0;
                    long frontVideoLength = 0;
                    for (int i = 0; i < ffVideoDecoders.size(); i ++) {
                        if (seekTime - frontVideoLength <= nativeDecoder.getVideoLength(ffVideoDecoders.get(i))) {
                            decoderIndex = i;
                            break;
                        } else {
                            frontVideoLength += nativeDecoder.getVideoLength(ffVideoDecoders.get(i));
                        }
                    }
                    int ffVideoDecoder = ffVideoDecoders.get(decoderIndex);

                    if (isDecodeStop) {
                        decodeLock.readLock().unlock();

                        if (decoderListener != null) {
                            decoderListener.decoderStop(VideoAccurateSeekDecoder.this);
                        }
                        return;
                    }

                    // 解码前一个I帧和后一个I帧
                    VideoFrame forwardIFrame = null;
                    VideoFrame backwardIFrame = null;

                    if (ffVideoDecoder != 0) {

                        Log.e(TAG, "解码前一个I帧 " + (seekTime - frontVideoLength + 100));
                        nativeDecoder.forwardSeekTo(ffVideoDecoder, seekTime - frontVideoLength + 100);

                        for (int i = 0; i < 10; i++) {
                            VideoFrame[] frames = nativeDecoder.decodeAFrame(ffVideoDecoder);
                            if (frames.length == 0) {
                                SystemClock.sleep(1);
                            } else {
                                backwardIFrame = frames[0];
                                backwardIFrame.globalPts = backwardIFrame.pts + frontVideoLength;
                                backwardIFrame.globalLength = getTotalVideoLength();
                                break;
                            }
                        }

                        if (backwardIFrame == null) {
                            Log.e(TAG, "解码前一个I帧失败, 需要解码到视频结束");
                        }

                        Log.e(TAG, "解码后一个I帧 " + (seekTime - frontVideoLength - 100));
                        nativeDecoder.backwardSeekTo(ffVideoDecoder, seekTime - frontVideoLength - 100);

                        for (int i = 0; i < 10; i++) {
                            VideoFrame[] frames = nativeDecoder.decodeAFrame(ffVideoDecoder);
                            if (frames.length == 0) {
                                SystemClock.sleep(1);
                            } else {
                                forwardIFrame = frames[0];
                                forwardIFrame.globalPts = forwardIFrame.pts + frontVideoLength;
                                forwardIFrame.globalLength = getTotalVideoLength();

                                // 添加到帧预览缓存数组
                                if (cacheFrameList.size() > maxSizeOfCacheFrameList) {
                                    cacheFrameList.removeFirst();
                                }
                                cacheFrameList.addLast(forwardIFrame);
                                break;
                            }
                        }

                        if (forwardIFrame == null) {
                            Log.e(TAG, "解码后一个I帧失败");

                            decodeLock.readLock().unlock();

                            if (decoderListener != null) {
                                decoderListener.decoderStop(VideoAccurateSeekDecoder.this);
                            }

                            return;
                        }

                    } else {
                        decodeLock.readLock().unlock();

                        if (decoderListener != null) {
                            decoderListener.decoderStop(VideoAccurateSeekDecoder.this);
                        }

                        return;
                    }

                    // 解锁
                    decodeLock.readLock().unlock();

                    // 解码
                    boolean isDecoderFinish = false; // 是否解码完成

                    in:
                    while (true) {

                        // 加锁
                        decodeLock.readLock().lock();

                        if (isDecodeStop) {
                            decodeLock.readLock().unlock();

                            if (decoderListener != null) {
                                decoderListener.decoderStop(VideoAccurateSeekDecoder.this);
                            }

                            return;
                        }

                        // 判断是否还在seek的有效期中
                        boolean isCacheValid = false;
                        if (forwardIFrame.globalPts <= seekTime) {
                            if ((backwardIFrame == null && nativeDecoder.getVideoLength(ffVideoDecoder) > (seekTime - frontVideoLength)) || (backwardIFrame != null && backwardIFrame.globalPts > seekTime)) {
                                isCacheValid = true;
                            }
                        }
                        if (!isCacheValid) {
                            Log.e(TAG, "不在seek的有效期中");
                            decodeLock.readLock().unlock();
                            break;
                        }

                        // 如果seek位置更新了, 返回离seek最近的一帧数据
                        if (isSeekTimeUpdate) {

                            VideoFrame resultFrame = null;
                            double resultTimeInterval = 0;

                            for (VideoFrame frame: cacheFrameList) {

                                if (resultFrame == null) {
                                    resultFrame = frame;
                                    resultTimeInterval = Math.abs(seekTime - frame.globalPts);
                                } else {
                                    double timeInterval = Math.abs(seekTime - frame.globalPts);
                                    if (timeInterval < resultTimeInterval) {
                                        resultFrame = frame;
                                        resultTimeInterval = timeInterval;
                                    }
                                }
                            }

                            if (resultFrame != null) {
                                isSeekTimeUpdate = false;

                                if (decoderListener != null) {
                                    decoderListener.seekToFrameUpdate(VideoAccurateSeekDecoder.this, resultFrame);
                                }
                            }
                        }

                        // 是否解码任务完成
                        if (isDecoderFinish) {
                            // 解锁
                            decodeLock.readLock().unlock();

                            SystemClock.sleep(10);

                            continue;
                        }

                        // 解码数据
                        VideoFrame[] frames = nativeDecoder.decodeAFrame(ffVideoDecoder);

                        for (final VideoFrame frame : frames) {

                            // 设置全局 pts 和 length 数据
                            frame.globalPts = frame.pts;
                            for (int x = 0; x < ffVideoDecoders.size(); x++) {
                                if (ffVideoDecoder != ffVideoDecoders.get(x)) {
                                    frame.globalPts += videoLengths[x];
                                } else {
                                    break;
                                }
                            }
                            frame.globalLength = totalVideoLength;

                            // 添加到帧预览缓存数组
                            if (cacheFrameList.size() > maxSizeOfCacheFrameList) {
                                cacheFrameList.removeFirst();
                            }
                            cacheFrameList.addLast(frame);

                            // 是否到结尾
                            if (Math.abs(frame.pts + frame.duration - frame.length) < 10) {

                                decodeLock.readLock().unlock();

                                // 解码到最后一帧
                                Log.e(TAG, "解码到EOF");

                                isDecoderFinish = true;

                                continue in;
                            }

                            // 解码任务是否完成
                            if (backwardIFrame != null && backwardIFrame.pts == frame.pts) {
                                decodeLock.readLock().unlock();

                                // 解码任务完成
                                Log.e(TAG, "解码任务完成");

                                isDecoderFinish = true;

                                continue in;
                            }
                        }

                        // 解锁
                        decodeLock.readLock().unlock();
                        SystemClock.sleep(10);
                    }
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

    public void setDecoderListener(VideoAccurateSeekDecoderListener decoderListener) {
        this.decoderListener = decoderListener;
    }

    public void setSeekTime(double seekTime) {
        // 加锁
        decodeLock.writeLock().lock();

        this.seekTime = seekTime;
        this.isSeekTimeUpdate = true;

        // 解锁
        decodeLock.writeLock().unlock();
    }

    public double getTotalVideoLength() {
        return totalVideoLength;
    }

    /**
     * 视频精准Seek监听
     */
    public interface VideoAccurateSeekDecoderListener {
        /**
         * 视频帧数据更新
         */
        void seekToFrameUpdate(VideoAccurateSeekDecoder decoder, VideoFrame videoFrames);

        void decoderStop(VideoAccurateSeekDecoder decoder);
    }
}
