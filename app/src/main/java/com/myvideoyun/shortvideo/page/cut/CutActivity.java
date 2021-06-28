package com.myvideoyun.shortvideo.page.cut;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.myvideoyun.decoder.FFmpegCMD;
import com.myvideoyun.shortvideo.Constant;
import com.myvideoyun.shortvideo.customUI.FrameHorizontalView;
import com.myvideoyun.shortvideo.MVYVideoEffectHandler;
import com.myvideoyun.shortvideo.page.edit.EditActivity;
import com.myvideoyun.shortvideo.page.input.InputActivity;
import com.myvideoyun.shortvideo.page.record.RecordActivity;
import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;
import com.myvideoyun.shortvideo.recordTool.MVYMediaMuxer;
import com.myvideoyun.shortvideo.tools.ScreenTools;
import com.myvideoyun.shortvideo.tools.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import static java.lang.Thread.sleep;

/**
 * Created by yangshunfa on 2019/3/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class CutActivity extends AppCompatActivity
        implements SurfaceHolder.Callback, CutViewCallback
        , FrameHorizontalView.OnSlidingListener, MVYMediaMuxer.ProgressListener
        , MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnCompletionListener {

    public static final String RESULT_DATA = "music_result_data";
    private static final String TAG = "CutActivity";
    CutView cutView;

    // 预览
    SurfaceView surfaceView;

    // 数据处理
    MVYVideoEffectHandler effectHandler;

    private MediaPlayer mediaPlayer;
    private ArrayList<MediaInfoModel> medias;
    private MediaInfoModel mVideo;
    // 是否分离音频
    private volatile boolean isSplit = false;
    private float rangeStart;
    private float rangeEnd;
    // 视频开始总长度 seek to here
    private int totalStart;
    // 视频开始总长度，seek to here
    private int totalEnd;
    private MVYMediaMuxer mediaMuxer;
    private AlertDialog dialog;
    private boolean surfaceCreated = false;
    private boolean mediaPlayerPrepared = false;
    private boolean onStart;
    private String outputAudioPath;
    private String outputVideoPath;
    private String outputCutPath;
    private String extractVideoPath;

    private int ffmpeg_exec_ret = 0;
    private boolean cutMediaFileDone = false;
    private FFmpegCMD.OnExecCallback cbCutMediaDone = new FFmpegCMD.OnExecCallback() {
        @Override
        public void onExecuted(int ret) {
            ffmpeg_exec_ret = ret;
            cutMediaFileDone = true;
            Log.e(TAG, "ffmpeg clip media return" + ret);
        }
    };

    private boolean extractVideoDone = false;
    private FFmpegCMD.OnExecCallback cbExtractVideoDone = new FFmpegCMD.OnExecCallback() {
        @Override
        public void onExecuted(int ret) {
            ffmpeg_exec_ret = ret;
            extractVideoDone = true;
            Log.e(TAG, "ffmpeg extract video return" + ret);
        }
    };

    private boolean extractAudioDone = false;
    private FFmpegCMD.OnExecCallback cbExtractAudioDone = new FFmpegCMD.OnExecCallback() {
        @Override
        public void onExecuted(int ret) {
            ffmpeg_exec_ret = ret;
            extractAudioDone = true;
            Log.e(TAG, "ffmpeg extract audio return" + ret);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cutView = new CutView(getBaseContext());
//        cutView.callback = this;
        surfaceView = cutView.previewView;
//        suFrfaceView.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFit);
        surfaceView.getHolder().addCallback(this);
        setContentView(cutView);
        cutView.callback = this;
        medias = (ArrayList<MediaInfoModel>) getIntent().getSerializableExtra("medias");
        mVideo = medias.get(0);
        Log.d(TAG, "需要裁剪的视频：" + medias.toString());

        float maxLength = mVideo.videoSeconds >= 15000 ? 15000 : mVideo.videoSeconds;
        cutView.mRangeView.setMaxLength(maxLength);
        cutView.mRangeView.setMinLength(3000);
        cutView.horizontalView.setValue(maxLength, mVideo.videoSeconds);
        cutView.horizontalView.setSlidingListener(this);
        Log.d(TAG, "初始化设置裁剪框 max：" + maxLength);

        // 设置surface view 宽高
        Point screen = ScreenTools.getScreen(this);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        params.height = (int) (screen.x * mVideo.height / mVideo.width);
        params.width = screen.x;
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        params.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;

        String rotation = "0";
        // 获取视频方向
        try {
            MediaMetadataRetriever retr = new MediaMetadataRetriever();
            retr.setDataSource(mVideo.videoPath);
            rotation = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Toast.makeText(this, "无效视频地址", Toast.LENGTH_SHORT).show();
        }
        if ("270".equals(rotation) || "90".equals(rotation)){
            params.width = screen.x;
            params.height = (int) (screen.x * mVideo.width / mVideo.height);
        }
        surfaceView.setLayoutParams(params);

        // 初始化裁剪范围
        rangeStart = 0;
        rangeEnd = maxLength;
        // 初始化预览范围
        totalStart = 0;
        totalEnd = (int) maxLength;

        // 初始化音频分离器
        outputVideoPath = getExternalCacheDir().getAbsolutePath() + "/" + UUID.randomUUID().toString().replace("-", "") + ".mp4";
        outputAudioPath = getExternalCacheDir().getAbsolutePath() + "/" + UUID.randomUUID().toString().replace("-", "") + ".wav";
        mediaMuxer = new MVYMediaMuxer(outputVideoPath, outputAudioPath);
        mediaMuxer.setListener(this);

        // 初始化dialog
        dialog = new AlertDialog.Builder(this).setMessage("正在合成视频...").create();
        dialog.setCancelable(false);


        if (mVideo.videoPath != null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
//            mediaPlayer.setLooping(true);
            mediaPlayer.setOnCompletionListener(this);
            try {
                mediaPlayer.setDataSource(mVideo.videoPath);
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.onStart = true;
        play();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        if (mediaPlayer != null) mediaPlayer.pause();
        this.onStart = false;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopMedia();

        super.onDestroy();
    }

    /**
     * 停止音频并释放
     * 停止视频播放
     */
    private void stopMedia() {
        // 停止音乐播放器
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * 播放视频
     */
    private void play() {
        if (surfaceCreated && mediaPlayerPrepared && onStart) {
            Log.d(TAG, "播放视频");
            mediaPlayer.start();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.surfaceCreated = true;
        effectHandler = new MVYVideoEffectHandler(getApplicationContext());
        if (mediaPlayer != null) {
//            mediaPlayer.setSurface(holder.getSurface());
            mediaPlayer.setDisplay(holder);
            play();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.surfaceCreated = false;
        if (effectHandler != null) {
            effectHandler.destroy();
            effectHandler = null;
        }
    }

    // ---------- ui 事件回调 ---------- start -----
    @Override
    public void backwards() {
        // 退出按钮
        stopMedia();
        finish();
    }

    @Override
    public void next() {
        // 1 停止播放
        stopMedia();
        // 2 阻止用户点击页面
        dialog.show();
//        FileTools.copyFileTo(mVideo.videoPath, getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".mp4")
//        .map();
        new Thread(new Runnable() {
            @Override
            public void run() {
                execCutVideoCmd();
            }
        }).start();
    }

    private void ffmpeg_fail_handle(String msg){
        final String errMsg = msg;
        dismissDialog();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CutActivity.this, errMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }
    /**
     * 使用 ffmpeg 裁剪视频
     */
    private void execCutVideoCmd() {
        // 3 裁剪视频
//        String cmdConvertion = "ffmpeg -i output.mp4 -strict -2  -qscale 0 -intra keyoutput.mp4";// 可能需要先转换成帧内编码，以实现精确裁剪
        String startPointTime = StringUtils.convertToSecond((int) (totalStart + rangeStart));
        String needDuration = StringUtils.convertToSecond((int) (rangeEnd - rangeStart));
        outputCutPath = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".mp4";
        // ffmpeg -ss 10 -t 15 -accurate_seek -i test.mp4 -vcodec libx264 -acodec aac cut.mp4
//        String cmd = "ffmpeg -ss 10 -t 15 -accurate_seek -i test.mp4 -vcodec libx264 -acodec aac cut.mp4";
        String cmd = "ffmpeg -threads 4";
        if ((int) (totalStart + rangeStart) > 0)
            cmd += " -accurate_seek -ss " + startPointTime;
        if ((int) (rangeEnd - rangeStart) < mVideo.videoSeconds)
            cmd += " -t " + needDuration;
        cmd += " -err_detect ignore_err -i " + String.format("\"%s\"", mVideo.videoPath) + " -max_muxing_queue_size 400 -copyts -start_at_zero -b:v 20000K -vf transpose=2 -vcodec libx264 -preset ultrafast -acodec copy " + String.format("\"%s\"", outputCutPath);
//        String cmd = "ffmpeg -ss " + startPointTime + " -t " + needDuration + " -i " + mVideo.videoPath + " -vcodec copy -acodec copy " + outputCutPath;

        Log.e(TAG, "FFmpeg 裁剪视频命令 : " + cmd);
        try {
            FFmpegCMD.exec(cmd, cbCutMediaDone);
        } catch (Exception e) {
            e.printStackTrace();
            ffmpeg_fail_handle("exception during clipping video file");
            return;
        }

        // waiting the cut media done
        while(cutMediaFileDone == false){
            try {
                sleep(10);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        if (ffmpeg_exec_ret != 0) {
            ffmpeg_fail_handle("clip video file failed");
            return;
        }

        Log.e(TAG, "clip media done, update video path to cutted media: " + outputCutPath);
        medias.get(0).videoPath = outputCutPath;

        // extract video
        extractVideoPath = getExternalCacheDir().getAbsolutePath() + "/" + UUID.randomUUID().toString().replace("-", "") + ".mp4";
        String cutVideoCmd = "ffmpeg -i " + String.format("\"%s\"", medias.get(0).videoPath) + " -metadata:s:v:0 rotate=270 -an -vcodec copy " + String.format("\"%s\"", extractVideoPath);
        Log.e(TAG, "FFmpeg 分离视频命令 : " + cutVideoCmd);
        try {
            FFmpegCMD.exec(cutVideoCmd, cbExtractVideoDone);
        }
        catch(Exception e){
            e.printStackTrace();
            ffmpeg_fail_handle("exception during extracting video failed");
            return;
        }

        // waiting the cut media done
        while(extractVideoDone == false) {
            try {
                sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (ffmpeg_exec_ret != 0) {
            ffmpeg_fail_handle("extract video file failed");
            return;
        }
        Log.e(TAG, "extract video done, update video path to: " + extractVideoPath);
        medias.get(0).videoPath = extractVideoPath;

        final String outputAudioPath = getExternalCacheDir().getAbsolutePath() + "/" + UUID.randomUUID().toString().replace("-", "") + ".wav";
        // 使用裁剪的视频源进行抽取，莫用medias里边那个单独抽取视频流的视频
        String cutAudioCmd = "ffmpeg -i " + String.format("\"%s\"",outputCutPath) + " -vn -acodec pcm_s16le -ac 1 -ar 16000 " + String.format("\"%s\"",outputAudioPath);
        Log.e(TAG, "FFmpeg 分离音频命令 : " + cutAudioCmd);
        try{
            FFmpegCMD.exec(cutAudioCmd, cbExtractAudioDone);
        }
        catch(Exception e){
            e.printStackTrace();
            ffmpeg_fail_handle("exception during extracting audio stream");
            return;
        }

        // waiting the cut media done
        while(extractAudioDone == false) {
            try {
                sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (ffmpeg_exec_ret != 0) {
            ffmpeg_fail_handle("extracting audio stream failed");
            return;
        }

        Log.e(TAG, "extract audio done, update audio path to: " + outputAudioPath);
        medias.get(0).audioPath = outputAudioPath;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                goEdit();
                dismissDialog();
            }
        });

    }

    /**
     * 关闭dialog
     */
    private void dismissDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog != null && dialog.isShowing()) dialog.dismiss();
            }
        });
    }

    /**
     * 进入编辑页面
     */
    private void goEdit() {

        if (Constant.shootMode == 1){

            // 带着视频开启新的 record 页面进行合拍
            Intent intent = new Intent(this, RecordActivity.class);
            // 将解码视频获得的视频参数带过去，不到下个页面重新解码获取
            String resolution = "720p";
            String ratio = "16:9";
            MediaInfoModel model = medias.get(0);
            int height = (int) model.height;
            int width = (int) model.width;

            if (height == 544) resolution = "540p";
            if (height == 1080) resolution = "1080p";
            if (height == width) ratio = "1:1";
            if (width / height == 0.75) ratio = "4:3";

            intent.putExtra(InputActivity.RESOLUTION, resolution);// resolution
            intent.putExtra(InputActivity.FRAME_RATE, "30");//frameRate
            intent.putExtra(InputActivity.VIDEO_BITRATE, "4096");//videoBitrate
            intent.putExtra(InputActivity.AUDIO_BITRATE, "64");//audioBitrate
            intent.putExtra(InputActivity.SCREEN_RATE, ratio);//screenRate
            intent.putExtra("duration", model.videoSeconds);//screenRate
            intent.putExtra("duet_media_path", model.videoPath);
            startActivity(intent);
            finish();
            return;
        }
        Intent data = new Intent(this, EditActivity.class);
        data.putExtra("medias", medias);
        startActivity(data);
        finish();
    }

    @Override
    public void cutChoiseStartPosition(float position) {
    }

    @Override
    public void cutChoiseEndPosition(float position) {
    }

    @Override
    public void cutFinish(float start, float end) {
        this.rangeStart = start;
        this.rangeEnd = end;
        Log.d(TAG, "cut video finish: start=" + start + " end=" + end + " , seek to: " + (totalStart + rangeStart));
        mediaPlayer.seekTo((int) (totalStart + rangeStart));
        mediaPlayer.start();
    }

    @Override
    public void cutChoosing() {
        // 滑动视频长度中，暂停播放
//        if (mediaPlayer != null){
        mediaPlayer.pause();
//        }
    }

    // 拖动进度条的回调
    @Override
    public void onSliding() {
        // 滑动视频长度中，暂停播放
//        if (mediaPlayer != null){
        mediaPlayer.pause();
//        }
    }

    @Override
    public void onSlideSelected(int start, int end) {
        // seek 当前需要显示的视频片段
        this.totalStart = start;
        this.totalEnd = end;
        Log.d(TAG, "原有的range : start=" + rangeStart + " end=" + rangeEnd);
        Log.d(TAG, "sliding selected:" + start + " end=" + end + " , seek to: " + (totalStart + rangeStart));
        mediaPlayer.seekTo((int) (totalStart + rangeStart));
        mediaPlayer.start();
    }
    // ---------- ui 事件回调 ---------- end -----

    // 音视频分离回调函数 ===== start =====
    @Override
    public void onProgress(float present) {

    }

    @Override
    public void onFailure(String message) {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        Log.d(TAG, "分离音视频分离失败" + message);
    }

    @Override
    public void onVideoSucceed(String path) {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        Log.d(TAG, "视频分离成功， path=" + path);
    }

    @Override
    public void onAudioSucceed(String path) {
        // 5 完成分离，进入EditActivity
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        Log.d(TAG, "音频分离成功， path=" + path);
        medias.get(0).audioPath = path;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // mediaplayer prepared 监听
        this.mediaPlayerPrepared = true;
        play();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
//        Log.d(TAG, "seek to " + (totalStart + rangeStart) + " is complete");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion");
        mediaPlayer.seekTo((int) (totalStart + rangeStart));
        mediaPlayer.start();
    }
    // 音视频分离回调函数 ===== end =====
}