package com.myvideoyun.shortvideo.page.cutmusic;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.myvideoyun.decoder.FFmpegCMD;
import com.myvideoyun.decoder.VideoDecoder;
import com.myvideoyun.decoder.VideoFrame;
import com.myvideoyun.shortvideo.MVYVideoEffectHandler;
import com.myvideoyun.shortvideo.page.effect.EffectRestore;
import com.myvideoyun.shortvideo.page.music.model.MediaModel;
import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;
import com.myvideoyun.shortvideo.MVYPreviewView;
import com.myvideoyun.shortvideo.tools.FileTools;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;

import static com.myvideoyun.shortvideo.tools.StringUtils.convertToSecond;

/**
 * Created by yangshunfa on 2019/3/6.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
@Deprecated
public class CutMusicActivity extends AppCompatActivity
        implements SurfaceHolder.Callback, VideoDecoder.VideoDecoderListener
        , VideoDecoder.VideoPlaybackListener
        , CutViewCallback {

    public static final String RESULT_DATA = "music_result_data";
    private static final String TAG = "CutMusicActivity";
    CutMusicView cutView;

    // 预览
    MVYPreviewView surfaceView;

    // 数据处理
    MVYVideoEffectHandler effectHandler;

    // 解码
    VideoDecoder videoDecoder;

//    private String iFrameVideoPath;
//    private String videoPath;
//    private String audioPath;

    // 是否是倒序解码
//    private boolean isReverseDecode;
    private MediaPlayer mediaPlayer;
    private ArrayList<MediaInfoModel> medias;
    private MediaModel music;
    private float startDuration;
    private float endDuration;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化 view
        cutView = new CutMusicView(getBaseContext());
//        cutView.callback = this;
        surfaceView = cutView.previewView;
        surfaceView.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFit);
        surfaceView.getHolder().addCallback(this);
        cutView.callback = this;
        setContentView(cutView);

        alertDialog = new AlertDialog.Builder(this).setMessage("裁剪音乐中...").create();
        alertDialog.setCancelable(false);

        // 初始化数据
        medias = (ArrayList<MediaInfoModel>) getIntent().getSerializableExtra("medias");
        music = (MediaModel) getIntent().getSerializableExtra("music");
        Log.d(TAG, "视频：" + medias.toString());
        Log.d(TAG, "需要裁剪的音乐：" + medias.toString());

        // 计算视频总长度及设置view
        float videoDuration = 0;// 视频总长度
        String[] mediaPaths = new String[medias.size()];
        for (int i = 0; i < medias.size(); i++) {
            videoDuration += medias.get(i).videoSeconds;
            mediaPaths[i] = medias.get(i).videoPath;
        }
        cutView.mRangeView.setMaxLength(music.duration);
        cutView.mRangeView.setMinLength(videoDuration);
        Log.d(TAG, "music 总长度 ：" + music.duration + ", need duration =" + videoDuration);

        videoDecoder = new VideoDecoder(true);
        videoDecoder.setDecoderListener(this);
        videoDecoder.setPlaybackListener(this);

        // 创建本地解码器
        videoDecoder.createNativeVideoDecoder(mediaPaths);

        startDuration = 0;
        endDuration = videoDuration;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 准备播放
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(music.path);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 开始播放
        videoDecoder.startDecodeFromFirstFrame();
        videoDecoder.startPlayer();
        mediaPlayer.start();
    }

    @Override
    protected void onStop() {
        stopMedia();
        super.onStop();
    }

    /**
     * 停止音乐播放和视频解码
     */
    private void stopMedia() {
        if (videoDecoder != null) {
            videoDecoder.stopDecoder();
            videoDecoder.stopPlayer();
        }
        if (mediaPlayer != null) mediaPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        destroyMedia();

        super.onDestroy();
    }

    @Override
    public void finish() {
        destroyMedia();
        super.finish();
    }

    @Override
    public void onBackPressed() {
        destroyMedia();
        super.onBackPressed();
    }

    /**
     * 销毁音视频播放器和解码器
     */
    private void destroyMedia() {
        // 停止音乐播放器
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // 停止解码
        if (videoDecoder != null) {
            videoDecoder.stopDecoder();
            videoDecoder.stopPlayer();
            // 关闭解码器
            videoDecoder.destroyNativeVideoDecoder();
            videoDecoder = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        effectHandler = new MVYVideoEffectHandler(getApplicationContext());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (effectHandler != null) {
            effectHandler.destroy();
            effectHandler = null;
        }
    }

    @Override
    public List<VideoFrame> decoderVideoOutput(VideoDecoder decoder, VideoFrame videoFrame) {

        List<VideoFrame> frames = new ArrayList<>();
        frames.add(videoFrame);
        return frames;
    }

    @Override
    public void decoderStop(VideoDecoder decoder) {
        Log.d(TAG, "decoderStop");
    }

    @Override
    public void decoderFinish(VideoDecoder decoder) {
        Log.d(TAG, "decoderFinish");
    }

    @Override
    public void playbackVideoOutput(VideoDecoder decoder, final VideoFrame frame) {

        // 渲染yuv数据到纹理
        if (effectHandler != null) {
            int texture;
            EffectRestore.setEffectHandlerType(effectHandler, frame, null);
            if (frame.rotate == 90) {
                texture = effectHandler.processWithYUVData(frame.yData, frame.uData, frame.vData, frame.width, frame.height, frame.lineSize, MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateRight);
            } else {
                texture = effectHandler.processWithYUVData(frame.yData, frame.uData, frame.vData, frame.width, frame.height, frame.lineSize, MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageNoRotation);
            }

            // 渲染到surfaceView
            if (surfaceView != null) {
                if (frame.rotate == 90) {
                    surfaceView.render(texture, frame.height, frame.width);
                } else {
                    surfaceView.render(texture, frame.width, frame.height);
                }
            }
        }
    }

    @Override
    public void playbackStop(VideoDecoder decoder) {
        Log.d(TAG, "playbackStop");
    }

    @Override
    public void playbackFinish(VideoDecoder decoder) {
        Log.d(TAG, "playbackFinish");

        // 正序解码
        videoDecoder.startDecodeFromFirstFrame();
        videoDecoder.startPlayer();
    }

    @Override
    public void backwards() {
        finish();
    }

    @Override
    public void next() {
        // 先裁剪音乐，生成新的音乐后回调
        final String newPath = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + music.path.substring(music.path.lastIndexOf("."));

        stopMedia();
        alertDialog.show();
        FileTools.copyFileTo(music.path, newPath)
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String s) throws Exception {
                        return s;
                    }
                })
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        final String newAudioPath = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".wav";
                        String startPointTime = convertToSecond((int) startDuration);// 00:00:00
                        String needDuration = convertToSecond((int) (endDuration - startDuration));//00:00:10
                        String cmd = "ffmpeg -i " + String.format("\"%s\"",newPath) + " -ss " + startPointTime + " -t " + needDuration + " -acodec pcm_s16le -ac 1 -ar 16000 " + String.format("\"%s\"",newAudioPath);

                        Log.e(TAG, "FFmpeg 命令 : " + cmd);

                        FFmpegCMD.exec(cmd, new FFmpegCMD.OnExecCallback() {
                            @Override
                            public void onExecuted(int ret) {
                                Log.e(TAG, "裁剪音乐结果：" + ret);
                                if (ret == 0) {
                                    Intent data = new Intent();
                                    music.path = newAudioPath;
                                    music.start = 0;
                                    music.end = endDuration - startDuration;
                                    data.putExtra("music", music);
                                    setResult(RESULT_OK, data);
                                    finish();
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(CutMusicActivity.this, "选取音乐失败", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                dismissDialog();
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        dismissDialog();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void dismissDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (alertDialog != null && alertDialog.isShowing()) alertDialog.dismiss();
            }
        });
    }

    @Override
    public void cutChoiseStart(float position) {
        videoDecoder.stopDecoder();
        videoDecoder.stopPlayer();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
    }

    @Override
    public void cutChoiseEnd(float start, float end) {
        this.startDuration = start;
        this.endDuration = end;
        videoDecoder.startDecodeFromFirstFrame();
        videoDecoder.startPlayer();
        mediaPlayer.seekTo((int) start);
        mediaPlayer.start();
    }

    @Override
    public void cutDragging(float start, float end) {
    }

}