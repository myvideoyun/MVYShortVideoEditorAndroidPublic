package com.myvideoyun.shortvideo.recordTool;

import android.content.Context;

import com.myvideoyun.soundtouch.SoundTouch;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by 汪洋 on 2019/2/21.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class MVYWavAudioRecord {

    private FileOutputStream fos;
    private String pcmFilePath;
    private String wavFilePath;
    private String tempoWavFilePath;
    private int sampleRate;
    private int channel;
    private float speedRate;

    public MVYWavAudioRecord(Context context, String path, int sampleRate, int channel, float speedRate) {
        this.pcmFilePath = context.getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-","") + ".pcm";
        this.wavFilePath = context.getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-","") + ".pcm";
        this.tempoWavFilePath = path;
        this.sampleRate = sampleRate;
        this.channel = channel;
        this.speedRate = speedRate;

        try {
            fos = new FileOutputStream(new File(pcmFilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void writePCMByteBuffer(ByteBuffer buffer) {
        try {
            if (fos != null) {
                byte[] bytes = new byte[buffer.limit()];
                buffer.get(bytes, 0, buffer.limit());
                fos.write(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void finish() {
        if (fos != null) {
            closeQuietly(fos);
            fos = null;

            generateWaveFile(pcmFilePath, wavFilePath);

            // 音频重设置节奏
            SoundTouch touch = new SoundTouch();
            touch.setTempo(speedRate);
            touch.processFile(wavFilePath, tempoWavFilePath);
        }
    }

    private void generateWaveFile(String rawFilePath, String wavFilePath) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = 0;
        long longSampleRate = sampleRate;
        int channels = channel;
        long byteRate = 16 * sampleRate * channels / 8;
        byte[] data = new byte[1024];
        try {
            in = new FileInputStream(rawFilePath);
            out = new FileOutputStream(wavFilePath);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            int n;
            while ((n = in.read(data)) != -1) {
                out.write(data, 0, n);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            closeQuietly(out);
            closeQuietly(in);

            File file = new File(rawFilePath);

            try {
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    private static void closeQuietly(Closeable closeable){
        if(closeable != null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
