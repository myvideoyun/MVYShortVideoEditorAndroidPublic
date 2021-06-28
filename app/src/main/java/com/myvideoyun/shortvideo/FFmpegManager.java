package com.myvideoyun.shortvideo;

import android.util.Log;

import com.myvideoyun.decoder.FFmpegCMD;

import java.util.UUID;

public class FFmpegManager {
    private static final String TAG = "FFmpegManager";
    public static String executeCombieMultipleVideo(String videoPath1, String videoPath2,int width, int height, FFmpegCMD.OnExecCallback callback){
        int w = width / 2;
        int h = height / 2;
        String combineResolution = 2 * h + "x" + w;
        String scale =  h + "x" + w;
        final String outputVideoPath = MVYApplication.instance.getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".mp4";
        // ffmpeg -i v1.mp4 -i v2.mp4 -filter_complex "nullsrc=size=1280x720 [base];[0:v] setpts=PTS-STARTPTS, scale=720x640 [upperleft];[1:v]setpts=PTS-STARTPTS,scale=720x640[upperright];[base][upperleft]overlay=shortest=1[tmp1];[tmp1][upperright]overlay=shortest=1:x=320" output.mp4
        String cmd = "ffmpeg -i " + String.format("\"%s\"",videoPath1) + " -i " + String.format("\"%s\"",videoPath2) + " -filter_complex nullsrc=size=" + combineResolution + "[base];[0:v]scale=" + scale + "[a];[1:v]scale=" + scale + "[b];[base][a]overlay=shortest=1[tmp1];[tmp1][b]overlay=shortest=1:x=" + h + " " + String.format("\"%s\"",outputVideoPath);
//        String cmd = "ffmpeg -i " + videoPath1 + " -i " + videoPath2 + " -filter_complex nullsrc=size=720x640[base];[0:v]scale=360x640[a];[1:v]scale=360x640[b];[base][a]overlay=shortest=1[tmp1];[tmp1][b]overlay=shortest=1:x=360 " + outputVideoPath;
        Log.e(TAG, "ffmpeg 多路合并视频命令：" + cmd);
        FFmpegCMD.exec(cmd, callback);
        return outputVideoPath;
    }
}
