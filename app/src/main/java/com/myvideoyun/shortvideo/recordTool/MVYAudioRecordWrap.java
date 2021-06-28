package com.myvideoyun.shortvideo.recordTool;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by 汪洋 on 2019/2/11.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class MVYAudioRecordWrap {

    private MVYAudioRecordListener audioRecordListener;

    private AudioRecord audioRecord;
    private int bufferSize;

    private boolean isStop;

    private Lock lock = new ReentrantLock();

    public MVYAudioRecordWrap(AudioRecord audioRecord, int bufferSize) {
        this.audioRecord = audioRecord;
        this.bufferSize = bufferSize;
    }

    public void startRecording() {
        audioRecord.startRecording();
        isStop = false;

        new Thread() {
            @Override
            public void run() {
                ByteBuffer audioBuffer=ByteBuffer.allocateDirect(bufferSize);

                long timestamp = SystemClock.elapsedRealtimeNanos() / 1000;

                while (true) {

                    lock.lock();

                    if (isStop) {
                        lock.unlock();
                        return;
                    }
                    audioBuffer.clear();

                    int readSize = audioRecord.read(audioBuffer, bufferSize);
                    if (readSize != AudioRecord.ERROR_INVALID_OPERATION) {

                        int perframeSize = 1;
                        if (audioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_FLOAT) {
                            perframeSize = 4;
                        } else if (audioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) {
                            perframeSize = 2;
                        } else if (audioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_8BIT) {
                            perframeSize = 1;
                        }

                        float timeInterval = readSize / audioRecord.getChannelCount() / perframeSize / (float)audioRecord.getSampleRate();

                        timestamp = (long) (timestamp + (timeInterval * 1000 * 1000));

                        if (audioRecordListener != null) {
                            audioRecordListener.audioRecordOutput(audioBuffer, timestamp);
                        }
                    }

                    lock.unlock();

                    // 休息一会, 释放锁资源
                    SystemClock.sleep(1);
                }
            }
        }.start();
    }

    public void stop() {
        lock.lock();

        audioRecord.stop();
        isStop = true;

        lock.unlock();
    }

    public void setAudioRecordListener(MVYAudioRecordListener audioRecordListener) {
        this.audioRecordListener = audioRecordListener;
    }
}

