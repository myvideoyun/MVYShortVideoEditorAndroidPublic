package com.myvideoyun.decoder;

import java.util.ArrayList;
import java.util.List;

public class FFmpegCMD {
    static {
        System.loadLibrary("mediadecoder");
        System.loadLibrary("ijkffmpeg");
    }

    // 执行FFmpeg命令
    public static void exec(String cmd, OnExecCallback callback) {
        List<String> cmdData = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (String cmdSpan : cmd.split(" ")) {

            if (cmdSpan.startsWith("\"")) {
                sb.append(cmdSpan);
            } else if (sb.length() > 0) {
                sb.append(" ");
                sb.append(cmdSpan);
            } else {
                cmdData.add(cmdSpan);
            }

            if (sb.toString().endsWith("\"")) {
                cmdData.add(sb.toString().substring(1, sb.length() - 1));
                sb = new StringBuilder();
            }
        }

        exec(cmdData.size(), cmdData.toArray(new String[0]), callback);
    }

    // 裁剪视频
    public static String cutVideoCMD(String startPointTime, String needDuration, String inputMediaPath, String outputMediaPath) {
        return "ffmpeg -threads 4 " +
                "-accurate_seek " +
                "-ss " + startPointTime + " " +
                "-t " + needDuration + " " +
                "-err_detect ignore_err " +
                "-i " + String.format("\"%s\"", inputMediaPath) + " " +
                "-r 30 " +
                "-b:v 20000K " +
                "-vf transpose=2 " +
                "-vcodec libx264 " +
                "-preset ultrafast " +
                "-acodec aac " +
                String.format("\"%s\"", outputMediaPath);
    }

    // 裁剪音频
    public static String cutAudioCMD(String startPointTime, String needDuration, String inputAudioPath, String outputAudioPath) {
        return "ffmpeg -threads 4 " +
                "-accurate_seek " +
                "-ss " + startPointTime + " " +
                "-t " + needDuration + " " +
                "-err_detect ignore_err " +
                "-i " + String.format("\"%s\"", inputAudioPath) + " " +
                "-acodec pcm_s16le " +
                "-ac 1 " +
                "-ar 16000 " +
                String.format("\"%s\"",outputAudioPath);
    }

    // 分离视频
    public static String separateVideoCMD(String inputMediaPath, String outputVideoPath) {
        return "ffmpeg -threads 4 " +
                "-i " + String.format("\"%s\"", inputMediaPath) + " " +
                "-metadata:s:v:0 rotate=270 " +
                "-an " +
                "-vcodec copy " +
                String.format("\"%s\"",outputVideoPath);
    }

    // 分离音频
    public static String separateAudioCMD(String inputMediaPath, String outputAudioPath) {
        return "ffmpeg -threads 4 " +
                "-i " + String.format("\"%s\"",inputMediaPath) + " " +
                "-vn " +
                "-acodec pcm_s16le " +
                "-ac 1 " +
                "-ar 16000 " +
                String.format("\"%s\"",outputAudioPath);
    }

    // 设置音量
    public static String increaseVolumeCMD(String volume, String inputAudioPath, String outputAudioPath) {
        return "ffmpeg -threads 4 " +
                "-i " + String.format("\"%s\"", inputAudioPath) + " " +
                "-filter:a volume=" + volume + " " +
                "-acodec pcm_s16le " +
                "-ac 1 " +
                "-ar 16000 " +
                String.format("\"%s\"", outputAudioPath);
    }

    // 拼接音频
    public static String concatAudioCMD(String[] inputAudioPath, String outputAudioPath) {
        StringBuilder inputSB = new StringBuilder();
        StringBuilder filterSB = new StringBuilder();
        for (int i = 0; i < inputAudioPath.length; i++) {
            inputSB.append(" -i ");
            inputSB.append(String.format("\"%s\"", inputAudioPath[i]));
            filterSB.append("[");
            filterSB.append(i);
            filterSB.append(":0]");
        }
        return "ffmpeg -threads 4 " +
                inputSB.toString() + " " +
                "-filter_complex " + filterSB.toString() + "concat=n=" + inputAudioPath.length + ":v=0:a=1[out] " +
                "-map [out] " +
                String.format("\"%s\"",outputAudioPath);
    }

    // 混合音频
    public static String mixAudioCMD(String inputMajorAudioPath, String inputMinorAudioPath, String outputAudioPath) {
        return "ffmpeg -threads 4 " +
                "-i " + String.format("\"%s\"",inputMajorAudioPath) + " " +
                "-i " + String.format("\"%s\"",inputMinorAudioPath) + " " +
                "-filter_complex amix=inputs=2:duration=first:dropout_transition=2 " +
                String.format("\"%s\"",outputAudioPath);
    }

    public interface OnExecCallback {
        void onExecuted(int ret);
    }

    public static native int exec(int argc, String[] argv, OnExecCallback listener);

}
