package com.myvideoyun.shortvideo.page.output;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Toast;

import com.myvideoyun.decoder.AudioDecoder;
import com.myvideoyun.decoder.AudioFrame;
import com.myvideoyun.decoder.FFmpegCMD;
import com.myvideoyun.decoder.VideoFrame;
import com.myvideoyun.shortvideo.FFmpegManager;
import com.myvideoyun.shortvideo.MVYVideoEffectHandler;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.page.edit.EditActivity;
import com.myvideoyun.shortvideo.page.edit.StickerRestore;
import com.myvideoyun.shortvideo.page.effect.EffectRestore;
import com.myvideoyun.shortvideo.page.input.InputActivity;
import com.myvideoyun.shortvideo.page.music.model.MediaModel;
import com.myvideoyun.shortvideo.page.record.RecordActivity;
import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;
import com.myvideoyun.shortvideo.recordTool.MVYMediaCodec;
import com.myvideoyun.shortvideo.recordTool.MVYMediaCodecHelper;
import com.myvideoyun.decoder.VideoDecoder;
import com.myvideoyun.shortvideo.MVYPreviewView;
import com.myvideoyun.shortvideo.tools.FileTools;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.UUID;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;

import static com.myvideoyun.shortvideo.recordTool.MVYMediaCodecHelper.getAvcSupportedFormatInfo;
import static com.myvideoyun.shortvideo.tools.StringUtils.convertToSecond;
import static java.lang.Thread.sleep;

/**
 * Created by 汪洋 on 2019/2/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class OutputActivity extends AppCompatActivity implements OutputViewCallback, SurfaceHolder.Callback, VideoDecoder.VideoDecoderListener, AudioDecoder.AudioDecoderListener {

    private static final String TAG = "OutputActivity";
    private static final int REQUEST_CODE_STORAGE = 1001;

    // UI
    OutputView outputView;

    // 预览
    MVYPreviewView surfaceView;

    // 画面处理
    MVYVideoEffectHandler effectHandler;

    // 解码
    VideoDecoder videoDecoder;
    boolean videoDecoderFinish = false;

    // 解码原声
    AudioDecoder originalBgAudioDecoder;
    boolean bgAudioDecoderFinish = false;

    // 编码
    MVYMediaCodec mediaCodec;
    boolean videoCodecConfigResult;
    boolean audioCodecConfigResult;

    // 导入的视频路径
    String outputMediaPath;

    // 输入的视频
    ArrayList<MediaInfoModel> medias;
    private String[] mVideoPaths;
//    private String[] originalAudioPaths;
    private int height = 720;
    private int width = 1280;
    private MediaModel music;

    // 测试seek
    Timer timer = new Timer();
    // 多个录制音频合并后的路径
    private String mergeAudioPath;
    // 录制的音频跟选择的音乐音频合并后的路径
    private String combineMusicPath;

    // 设置音量后的音频
    String setOriginalVolumePath = "" ;
    // 是指音乐音量后的音频
    String setMusicVolumePath = "" ;
    // 视频总长度
    private float amountDuration;
    private AlertDialog alertDialog;
    private float musicVolume;
    private float originalValume;
    private String duetPath;
    // 合拍合成后视频路径
    private String combinedDuetPath;

    // flag to indicate ffmpeg task done;
    private int ffmpegTaskCompleted = 0;
    private int lastFFmpegCmdRet = 0;
    private void waitForFfmpegDone(){
        // waiting the cut media done
        while(ffmpegTaskCompleted == 0) {
            try {
                sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void clearFFmpegCompletedFlag(){
        ffmpegTaskCompleted = 0;
    }

    FFmpegCMD.OnExecCallback ffmpegNotifyCallback = new FFmpegCMD.OnExecCallback() {
        @Override
        public void onExecuted(int ret) {
            Log.e(TAG, "Complete ffmpeg task：" + ret);
            ffmpegTaskCompleted = 1;
            lastFFmpegCmdRet = ret;
        }
    };

    private void execFFmpegCmdSync(String cmd, String Msg){
        Log.e(TAG, Msg + cmd);
        clearFFmpegCompletedFlag();
        FFmpegCMD.exec(cmd, ffmpegNotifyCallback);
        waitForFfmpegDone();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        outputView = new OutputView(getBaseContext());
        outputView.callback = this;
        setContentView(outputView);

        surfaceView = outputView.preview;
        surfaceView.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFit);
        surfaceView.getHolder().addCallback(this);

        Intent intent = getIntent();
        medias = (ArrayList<MediaInfoModel>) intent.getSerializableExtra("medias");
        music = (MediaModel) intent.getSerializableExtra("music");
        musicVolume = intent.getFloatExtra(EditActivity.INTENT_DATA_MUSIC_VOLUME, -1);
        originalValume = intent.getFloatExtra(EditActivity.INTENT_DATA_ORIGINAL_VOLUME, -1);
        duetPath = intent.getStringExtra("duet_path");
        Log.e(TAG, "需要的音量：music="+ musicVolume + ", audio=" + originalValume);
        if (!TextUtils.isEmpty(duetPath)){
            outputView.duetBtn.setVisibility(View.GONE);
        }

        outputMediaPath = getExternalCacheDir() +  "/" + UUID.randomUUID().toString().replace("-","") + ".mp4";

        MediaInfoModel tempM;
        mVideoPaths = new String[medias.size()];
//        originalAudioPaths = new String[medias.size()];
        for (int i= 0 ; i< medias.size() ;i++){
            tempM = medias.get(i);
            mVideoPaths[i] = tempM.videoPath;
//            originalAudioPaths[i] = tempM.audioPath;
            amountDuration += tempM.videoSeconds;
        }


        alertDialog = new AlertDialog.Builder(this).setMessage("正在合成视频，请勿退出当前页面").create();
        alertDialog.setCancelable(false);

        StickerRestore.initAdded();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy =====");
        stopMediaAndCodec();
        destroyDecoder();
        super.onDestroy();
    }

    @Override
    public void finish() {
        stopMediaAndCodec();
        destroyDecoder();
        Log.e(TAG, "finish =====");
        super.finish();
    }

    @Override
    public void onBackPressed() {
        Log.e(TAG, "onBackPressed =====");
        stopMediaAndCodec();
        destroyDecoder();
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            insertVideoToAlbum(outputMediaPath);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        effectHandler = new MVYVideoEffectHandler(getApplicationContext());
//        effectHandler.setTypeOfShortVideo(MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_NONE);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        effectHandler.destroy();
        effectHandler = null;
    }

    @Override
    public List<VideoFrame> decoderVideoOutput(VideoDecoder decoder, VideoFrame frame) {
        Log.e(TAG, "current pts=" + frame.pts);

        // 渲染yuv数据到纹理
        if (effectHandler != null) {
            int texture;

            if (EffectRestore.effects != null) EffectRestore.setEffectHandlerType(effectHandler, frame, null);
            StickerRestore.addAllStickerToVideo(effectHandler, frame);

            if (frame.rotate == 90) {
                texture = effectHandler.processWithYUVData(frame.yData, frame.uData, frame.vData, frame.width, frame.height, frame.lineSize, MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateRight);
            } else {
                texture = effectHandler.processWithYUVData(frame.yData, frame.uData, frame.vData, frame.width, frame.height, frame.lineSize, MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageNoRotation);
            }

            if (mediaCodec != null && videoCodecConfigResult) {
                Log.e(TAG, "current pts=" + frame.pts + ", frame.globalPts" + frame.globalPts );
                mediaCodec.writeImageTexture(texture, effectHandler.outputWidth, effectHandler.outputHeight, (long) (frame.globalPts * 1000* 1000));
//                mediaCodec.writeImageTexture(texture, effectHandler.outputWidth, effectHandler.outputHeight, (long) currentPts * 1000 * 1000);
//                height = frame.height;
//                width = frame.width;
            }

            // 渲染到画面
//            surfaceView.render(texture, effectHandler.outputWidth, effectHandler.outputHeight);
        }

        List<VideoFrame> frames = new ArrayList<>();
        frames.add(frame);
        return frames;
    }


    @Override
    public void decoderStop(VideoDecoder decoder) {

    }

    @Override
    public void decoderFinish(VideoDecoder decoder) {
        Log.d(TAG, "videoDecoderFinish");
        videoDecoderFinish = true;
        synthesizeMedia();
    }

    @Override
    public List<AudioFrame> decoderAudioOutput(AudioDecoder decoder, AudioFrame frame) {
        Log.d(TAG, "decoder audio " + frame.pts);

        if (mediaCodec != null && audioCodecConfigResult) {
            ByteBuffer audioBuffer=ByteBuffer.allocateDirect(frame.bufferSize);
            audioBuffer.put(frame.buffer);
            mediaCodec.writePCMByteBuffer(audioBuffer, (long) (frame.pts * 1000));
        }

        List<AudioFrame> frames = new ArrayList<>();
        frames.add(frame);
        return frames;
    }

    @Override
    public void decoderStop(AudioDecoder decoder) {

    }

    @Override
    public void decoderFinish(AudioDecoder decoder) {
        Log.d(TAG, "audioDecoderFinish");
        bgAudioDecoderFinish = true;
        synthesizeMedia();
    }

    // ui 回调
    @Override
    public void outputViewOnSave() {
        alertDialog.show();
        // ffmpeg 命令必须串联执行

        // 点击按钮开始合并视频分以下几步
        // 1. 如果有多段录制的音频，合并多段音频
        // 2. 如果设置了原声音量，设置音量
        // 2. 如果没有多段视频以及合并完多段视频，进入3
        // 3. 合并录制的音频以及选择的音乐（已裁剪），进入6
        // 4. 如果选择的音乐没裁剪，进入5
        // 5. 裁剪完音乐后，进入6
        // 6. 合并录制的音频和裁剪的音频
        // 7 编码最终的音频以及多个视频
        mergeAudios();
    }

    private boolean isDuet = false;
    // 合拍
    @Override
    public void duetShoot() {
        if (bgAudioDecoderFinish && videoCodecConfigResult){
            duet();
        } else{
            isDuet = true;
            // 先合成视频
            outputViewOnSave();
        }
    }

    private void duet() {
        // 带着视频开启新的 record 页面进行合拍
        Intent intent = new Intent(this, RecordActivity.class);
        // 将解码视频获得的视频参数带过去，不到下个页面重新解码获取
        String resolution = "720p";
        String ratio = "16:9";
        if (height == 544) resolution = "540p";
        if (height == 1080) resolution = "1080p";
        if (height == width) ratio = "1:1";
        if (medias.get(0).ratio == 0.75) ratio = "4:3";
        intent.putExtra(InputActivity.RESOLUTION, resolution);// resolution
        intent.putExtra(InputActivity.FRAME_RATE, "30");//frameRate
        intent.putExtra(InputActivity.VIDEO_BITRATE, "4096");//videoBitrate
        intent.putExtra(InputActivity.AUDIO_BITRATE, "64");//audioBitrate
        intent.putExtra(InputActivity.SCREEN_RATE, ratio);//screenRate
        intent.putExtra("duration", amountDuration);//screenRate
        intent.putExtra("duet_media_path", outputMediaPath);
        startActivity(intent);
    }

    /**
     * 设置音频音量命令
     * @param callback
     * @param audioPath
     * @param volume
     * @return
     */
    public String setValume(FFmpegCMD.OnExecCallback callback, String audioPath, float volume){
        final String newVolumeAudioPath = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".wav";
        String cmd = "ffmpeg -i " + String.format("\"%s\"",audioPath) + " -filter:a volume=" + volume + " -acodec pcm_s16le -ac 1 -ar 16000 " + String.format("\"%s\"",newVolumeAudioPath);
        Log.e(TAG, "ffmpeg 设置音量命令：" + cmd);
        //FFmpegCMD.exec(cmd, callback);
        execFFmpegCmdSync(cmd, "FFmpeg set volume: ");
        callback.onExecuted(lastFFmpegCmdRet);
        return newVolumeAudioPath;
    }

    /**
     * 1. 合并多段录制的音频
     */
    private void mergeAudios() {
        // 合并多段录制的音频
        if (medias.size() > 1) {
            // 拼接 ffmpeg 命令
            String audios = "";
            String filters = "";
            for (int i = 0; i < medias.size(); i++) {
                MediaInfoModel m = medias.get(i);
                audios += " -i " + String.format("\"%s\"",m.audioPath);
                filters += "[" + i + ":0]";
            }
            mergeAudioPath = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".wav";
            String cmd = "ffmpeg" + audios + " -filter_complex " + filters + "concat=n=" + medias.size() + ":v=0:a=1[out] -map [out] " + String.format("\"%s\"",mergeAudioPath);
            Log.e(TAG, "ffmpeg 拼接音频命令：" + cmd);
            execFFmpegCmdSync(cmd, "ffmpeg concatenate audio: ");
            if(lastFFmpegCmdRet == 0){
			    setOriginalValume();
			}
        } else {
            // 只有一段视频和音频
            mergeAudioPath = medias.get(0).audioPath;// 使用第一段音频即可
            setOriginalValume();
        }
    }

    /**
     * 2. 设置原声
     */
    public void setOriginalValume(){
        if (originalValume > -1){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setOriginalVolumePath = setValume(new FFmpegCMD.OnExecCallback() {
                        @Override
                        public void onExecuted(int ret) {
                            Log.e(TAG, "设置音量结果：" + ret);
                            if ( ret == 0){
                                mergeAudioPath = setOriginalVolumePath;
                            }
                            cutMusic();
                        }
                    }, mergeAudioPath, originalValume);
                }
            });
        } else {
            cutMusic();
        }
    }

    /**
     * 3. 如果音乐没有裁剪过，先裁剪
     */
    private void cutMusic() {
        if (music != null && TextUtils.isEmpty(music.cutPath)) {
            // 先裁剪音乐，生成新的音乐后回调
            final String newPath = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + music.path.substring(music.path.lastIndexOf("."));

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
                            String startPointTime = convertToSecond(0);// 00:00:00
                            String needDuration = convertToSecond((int) amountDuration);//00:00:10
                            String cmd = "ffmpeg -i " + String.format("\"%s\"",newPath) + " -ss " + startPointTime + " -t " + needDuration + " -acodec pcm_s16le -ac 1 -ar 16000 " + String.format("\"%s\"",newAudioPath);

                            Log.e(TAG, "FFmpeg 命令 : " + cmd);

                            execFFmpegCmdSync(cmd, "ffmpeg cut music: ");
                            if(lastFFmpegCmdRet == 0) {
                                music.cutPath = newAudioPath;
                                setMusicValume();
                            }
                            else
                                showToastInMainThread("裁剪音乐失败");
                        }

                        @Override
                        public void onError(Throwable e) {
                            showToastInMainThread("裁剪音乐失败");
                        }

                        @Override
                        public void onComplete() {
                        }
                    });
        } else {
            setMusicValume();
        }
    }

    /**
     * 4. 设置音乐音量
     */
    public void setMusicValume(){
        if (musicVolume > -1 && music != null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setMusicVolumePath = setValume(new FFmpegCMD.OnExecCallback() {
                        @Override
                        public void onExecuted(int ret) {
                            Log.e(TAG, "设置音量结果：" + ret);
                            if ( ret == 0){
                                music.cutPath = setMusicVolumePath;
                            }
                            combineMusicAndAudio();
                        }
                    }, music.cutPath, musicVolume);
                }
            });
        } else {
            combineMusicAndAudio();
        }
    }
    /**
     * 5. 合并录制的音频和选择的音乐
     */
    private void combineMusicAndAudio() {
        if (music != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    combineMusicPath = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".wav";
                    if(EffectRestore.isSlow)
                    {
                        String newMedia = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".wav";
                        String cmd = "ffmpeg -i " + String.format("\"%s\"", mergeAudioPath) + " -filter:a \"atempo=0.5\" " + String.format("\"%s\"",newMedia);
                        execFFmpegCmdSync(cmd, "FFmpeg slow down the audio");
                        mergeAudioPath = newMedia;
                    }
                    if(EffectRestore.isFast){
                        String newMedia = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".wav";
                        String cmd = "ffmpeg -i " + String.format("\"%s\"", mergeAudioPath) + " -filter:a \"atempo=2.0\" " + String.format("\"%s\"",newMedia);
                        execFFmpegCmdSync(cmd, "FFmpeg speed up: ");
                        mergeAudioPath = newMedia;
                    }

                    String cmd = "ffmpeg -i " + String.format("\"%s\"",mergeAudioPath) + " -i " + String.format("\"%s\"",music.cutPath) + " -filter_complex amix=inputs=2:duration=first:dropout_transition=2 " + String.format("\"%s\"",combineMusicPath);
                    Log.e(TAG, "ffmpeg 合并音频命令：" + cmd);
                    execFFmpegCmdSync(cmd, "FFmpeg combine music and audio: ");
                    if(lastFFmpegCmdRet == 0){
                        // 开始编码
                        startMediaCodec();
                        // 开始解码
                        startDecoder();
                    } else{
                        showToastInMainThread("合并音频失败");
                    }
                }
            });
        } else {
            if(EffectRestore.isSlow)
            {
                String newMedia = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".wav";
                String cmd = "ffmpeg -i " + String.format("\"%s\"", mergeAudioPath) + " -filter:a \"atempo=0.5\" " + String.format("\"%s\"",newMedia);
                execFFmpegCmdSync(cmd, "FFmpeg slow down the audio");
                mergeAudioPath = newMedia;
            }
            if(EffectRestore.isFast){
                String newMedia = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".wav";
                String cmd = "ffmpeg -i " + String.format("\"%s\"", mergeAudioPath) + " -filter:a \"atempo=2.0\" " + String.format("\"%s\"",newMedia);
                execFFmpegCmdSync(cmd, "FFmpeg speed up: ");
                mergeAudioPath = newMedia;
            }
            // 开始编码
            startMediaCodec();
            // 开始解码
            startDecoder();
        }
    }

    /**
     * 主线程提示吐司
     * @param msg
     */
    private void showToastInMainThread(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(OutputActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * 开始视频和音频解码
     */
    private void startDecoder() {

        MediaInfoModel media = medias.get(0);

        // 开始视频解码
        videoDecoder = new VideoDecoder(false);
        videoDecoder.setDecoderListener(this);
        videoDecoder.createNativeVideoDecoder(mVideoPaths);
        if (EffectRestore.isReverse) {
            videoDecoder.startDecodeFromLastFrame(amountDuration);
        }
        if(EffectRestore.isNormal){
            videoDecoder.startDecodeFromFirstFrame(0, 1.0f);
        }
        if(EffectRestore.isFast){
            videoDecoder.startFastDecoding();
        }
        if(EffectRestore.isSlow){
            videoDecoder.startSlowDecoding();
        }

        // 解码原声背景音乐
        originalBgAudioDecoder = new AudioDecoder(false);
        originalBgAudioDecoder.setDecoderListener(this);
//        originalBgAudioDecoder.createNativeAudioDecoder(originalAudioPaths);
        String [] audioPaths = new String[]{combineMusicPath};
        if (TextUtils.isEmpty(combineMusicPath)){
            audioPaths[0] = mergeAudioPath;
        }
        originalBgAudioDecoder.createNativeAudioDecoder(audioPaths);
        originalBgAudioDecoder.startDecodeFromFirstFrame();
    }

    /**
     * 启动编码器
     */
    private void startMediaCodec() {
        // 初始化生成音视频的参数

        String resolution = outputView.resolutionRadioGroup.getSelectedText();// 分辨率
        String videoBitrate = outputView.videoBitrateRadioGroup.getSelectedText();
        String audioBitrate = outputView.audioBitrateRadioGroup.getSelectedText();
        if (resolution != null) {
            switch (resolution) {
                case "540p":
                    width = 960;
                    height = 544;
                    break;
                case "720p":
                    width = 1280;
                    height = 720;
                    break;
                case "1080p":
                    width = 1920;
                    height = 1080;
                    break;
            }
        }
        if (medias.get(0).ratio == 0.75){
            if ("540p".equals(resolution)){
                width = 720;
            }
            if ("1080p".equals(resolution)){
                width = 1440;
            }
            if ("720p".equals(resolution)){
                width = 960;
            }
        } else if (medias.get(0).ratio == 1){
            width = height;
        }
        // 图像编码参数
//        int width = 1280; // 视频编码时图像旋转了90度
//        int height = 720;
        int bitRate = 2 * 1024 * 1024; // 码率: 2Mbps 2 * 1024 * 1024
//        int bitRate = Integer.decode(videoBitrate) * 1000; // 码率: 2Mbps 2 * 1024 * 1024
        int fps = 30; // 帧率: 30
        int iFrameInterval = 1; // GOP: 30

        // 音频编码参数
        // Integer.decode(audioBitrate) * 1000
        int audioBitRate = 16 * 1000; // 码率: 128kbps
        int audioSampleRate = 16000; // 采样率: 44.1k

        // 编码器信息
        MVYMediaCodecHelper.CodecInfo codecInfo = getAvcSupportedFormatInfo();
        if (codecInfo == null) {
            Log.d(TAG, "不支持硬编码");
            return;
        }

        // 设置给编码器的参数不能超过其最大值
        if (width > codecInfo.maxWidth) {
            width = codecInfo.maxWidth;
        }
        if (height > codecInfo.maxHeight) {
            height = codecInfo.maxHeight;
        }
        if (bitRate > codecInfo.bitRate) {
            bitRate = codecInfo.bitRate;
        }
        if (fps > codecInfo.fps) {
            fps = codecInfo.fps;
        }
        addStickerSubtitle();

        // 启动编码
        mediaCodec = new MVYMediaCodec(outputMediaPath, 2);
        mediaCodec.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFill);
        videoCodecConfigResult = mediaCodec.configureVideoCodecAndStart(width, height, bitRate, fps, iFrameInterval, 1);
        audioCodecConfigResult = mediaCodec.configureAudioCodecAndStart(audioBitRate, audioSampleRate);

        Log.d(TAG, "开始编码，初始化参数；" + "width = " + width + "height = " + height + "bitRate = " + bitRate
                + "fps = " + fps + "IFrameInterval = " + iFrameInterval + "speedRate = " + 1 + videoCodecConfigResult + audioCodecConfigResult);
    }

    /**
     * 添加贴纸和字幕
     */
    private void addStickerSubtitle() {
        if (effectHandler == null) return;
        // 效果
//        String stickerPath = getIntent().getStringExtra(EditActivity.INTENT_DATA_STICKER);
//        String subtilePath = getIntent().getStringExtra(EditActivity.INTENT_DATA_SUBTTILE);
//        Log.e(TAG, "需要添加的贴纸：" + stickerPath + ", 字幕：" + subtilePath);
//        if (!TextUtils.isEmpty(stickerPath) ){
//            try {
//                Bitmap stickerBitmap = BitmapFactory.decodeStream(getAssets().open(stickerPath));
//                MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerFilterModel = new MVYGPUImageStickerFilter.MVYGPUImageStickerModel(stickerBitmap);
//                effectHandler.addSticker(stickerFilterModel);
//
//                // 设置贴纸位置
//                Matrix.rotateM(stickerFilterModel.transformMatrix, 0, 0.f, 0.f, 0.f, 1.f);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        if (!TextUtils.isEmpty(subtilePath)){
//            Bitmap bitmap = BitmapFactory.decodeFile(subtilePath);
//            MVYGPUImageStickerFilter.MVYGPUImageStickerModel subtitleModel = new MVYGPUImageStickerFilter.MVYGPUImageStickerModel(bitmap);
//            //  设置字幕位置
//            Matrix.rotateM(subtitleModel.transformMatrix, 0, 0.f, 0.f, 0.f, 1.f);
//            Matrix.translateM(subtitleModel.transformMatrix, 0, 0.f, -0.6f, 0.0f);
//            effectHandler.addSticker(subtitleModel);
//        }
    }

    /**
     * 停止编码和解码
     */
    private void stopMediaAndCodec() {
        if (mediaCodec != null){
            mediaCodec.finish();
        }

        // 背景音乐解码
        if (originalBgAudioDecoder != null){
            originalBgAudioDecoder.stopDecoder();
            originalBgAudioDecoder.stopPlayer();
        }

    }

    /**
     * 销毁编码器及解码器
     */
    private void destroyDecoder() {
        if (mediaCodec != null){
            mediaCodec = null;
        }
        if (originalBgAudioDecoder != null){
            originalBgAudioDecoder.destroyNativeAudioDecoder();
            originalBgAudioDecoder = null;
        }
    }

    /**
     * 合成完成，进入播放演示
     */
    private void synthesizeMedia() {
        // mediaCodec 为空可能是因为点击了返回，
        if (videoDecoderFinish && bgAudioDecoderFinish && mediaCodec != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 关闭dialog
                    alertDialog.dismiss();
                    stopMediaAndCodec();
                    destroyDecoder();

                    for (MediaInfoModel m : medias) {
                        Log.e(TAG, "原视频：" + m.videoPath + ", 音频："+ m.audioPath);
                    }
                    Log.e(TAG, "最终音频, combineMusicPath:" + mergeAudioPath + ", combineMusicPath:" + combineMusicPath);
                    Log.e(TAG, "最终视频：" + outputMediaPath);
                    String gifPath = outputMediaPath + "_short.gif";
                    String cmd = "ffmpeg -ss 0 -t 2 -i " + String.format("\"%s\"",outputMediaPath) + " -vf \"fps=5,scale=320:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse\" " + String.format("\"%s\"",gifPath);
                    Log.e(TAG, "ffmpeg 合并音频命令：" + cmd);
                    execFFmpegCmdSync(cmd, "FFmpeg combine music and audio: ");

                    if (!TextUtils.isEmpty(duetPath)){
                        combineDuetAndMedia();
                    } else {
                        showVideo();
                    }
                }
            });
        }
    }

    /**
     * 插入视频到图库并使用系统播放
     */
    private void showVideo() {
        insertVideoToAlbum(outputMediaPath);
        if (isDuet){
            duet();
            isDuet = false;
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri data;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                data = FileProvider.getUriForFile(getBaseContext(), "com.aiyaapp.aiya.test.fileprovider", new File(outputMediaPath));
            } else {
                data = Uri.fromFile(new File(outputMediaPath));
            }
            intent.setDataAndType(data, "video/*");
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(OutputActivity.this, "合成完成：" + outputMediaPath, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 合并合拍视频
     */
    private void combineDuetAndMedia() {
        if (!TextUtils.isEmpty(duetPath)){
            alertDialog = new AlertDialog.Builder(this).setMessage("正在合成合拍视频").create();
            alertDialog.setCancelable(false);
            alertDialog.show();
            combinedDuetPath = FFmpegManager.executeCombieMultipleVideo(medias.get(0).videoPath, duetPath,width, height, new FFmpegCMD.OnExecCallback() {
                @Override
                public void onExecuted(int ret) {
                    Log.e(TAG, "合拍命令执行结束：" + ret + " , path=" + combinedDuetPath);
                    parseCombineDuetResult(ret);
                }
            });
            return;
        }
    }

    /**
     * 主线程中处理合并合拍视频的结果
     * @param ret
     */
    private void parseCombineDuetResult(final int ret) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.dismiss();
                if (ret == 0){
                    outputMediaPath = combinedDuetPath;
                    showVideo();
                } else {
                    Toast.makeText(OutputActivity.this, "合成失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 保存视频到系统图库
     * @param videoPath
     */
    public void insertVideoToAlbum(String videoPath){
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_CODE_STORAGE);
            return;
        }
//        File directory = new File(Environment.getExternalStorageDirectory().getPath() + "/mvy");
//        if (!directory.exists()) directory.mkdirs();
//        String newPath = directory.getPath() + "/mvy_" + UUID.randomUUID().toString().replace("-" , "") + ".mp4";
//        Log.e(TAG, "保存到相册地址：" + newPath);
//        FileTools.copyFileTo(videoPath, newPath).subscribe();

        File file = new File(videoPath);
        long timeMillis = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
        values.put(MediaStore.Video.VideoColumns.DISPLAY_NAME, file.getName());
        values.put(MediaStore.Video.VideoColumns.SIZE, file.length());
        values.put(MediaStore.Video.VideoColumns.DATE_ADDED, timeMillis);
        values.put(MediaStore.Video.VideoColumns.DATE_MODIFIED, timeMillis);
        values.put(MediaStore.Video.VideoColumns.DURATION, amountDuration);
        values.put(MediaStore.Video.VideoColumns.WIDTH, width);
        values.put(MediaStore.Video.VideoColumns.HEIGHT, height);
        values.put(MediaStore.MediaColumns.MIME_TYPE, getVideoMimeType(videoPath));
        getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }

    /**
     * 获取video的mine_type,支持mp4,3gp
     */
    private static String getVideoMimeType(String path) {
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith("mp4") || lowerPath.endsWith("mpeg4")) {
            return "video/mp4";
        } else if (lowerPath.endsWith("3gp")) {
            return "video/3gp";
        }
        return "video/mp4";
    }
}
