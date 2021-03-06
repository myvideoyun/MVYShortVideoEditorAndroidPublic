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
 * Created by ?????? on 2019/2/5.
 * Copyright ?? 2019??? myvideoyun. All rights reserved.
 */
public class OutputActivity extends AppCompatActivity implements OutputViewCallback, SurfaceHolder.Callback, VideoDecoder.VideoDecoderListener, AudioDecoder.AudioDecoderListener {

    private static final String TAG = "OutputActivity";
    private static final int REQUEST_CODE_STORAGE = 1001;

    // UI
    OutputView outputView;

    // ??????
    MVYPreviewView surfaceView;

    // ????????????
    MVYVideoEffectHandler effectHandler;

    // ??????
    VideoDecoder videoDecoder;
    boolean videoDecoderFinish = false;

    // ????????????
    AudioDecoder originalBgAudioDecoder;
    boolean bgAudioDecoderFinish = false;

    // ??????
    MVYMediaCodec mediaCodec;
    boolean videoCodecConfigResult;
    boolean audioCodecConfigResult;

    // ?????????????????????
    String outputMediaPath;

    // ???????????????
    ArrayList<MediaInfoModel> medias;
    private String[] mVideoPaths;
//    private String[] originalAudioPaths;
    private int height = 720;
    private int width = 1280;
    private MediaModel music;

    // ??????seek
    Timer timer = new Timer();
    // ????????????????????????????????????
    private String mergeAudioPath;
    // ?????????????????????????????????????????????????????????
    private String combineMusicPath;

    // ????????????????????????
    String setOriginalVolumePath = "" ;
    // ??????????????????????????????
    String setMusicVolumePath = "" ;
    // ???????????????
    private float amountDuration;
    private AlertDialog alertDialog;
    private float musicVolume;
    private float originalValume;
    private String duetPath;
    // ???????????????????????????
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
            Log.e(TAG, "Complete ffmpeg task???" + ret);
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
        Log.e(TAG, "??????????????????music="+ musicVolume + ", audio=" + originalValume);
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


        alertDialog = new AlertDialog.Builder(this).setMessage("?????????????????????????????????????????????").create();
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

        // ??????yuv???????????????
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

            // ???????????????
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

    // ui ??????
    @Override
    public void outputViewOnSave() {
        alertDialog.show();
        // ffmpeg ????????????????????????

        // ?????????????????????????????????????????????
        // 1. ???????????????????????????????????????????????????
        // 2. ??????????????????????????????????????????
        // 2. ????????????????????????????????????????????????????????????3
        // 3. ??????????????????????????????????????????????????????????????????6
        // 4. ???????????????????????????????????????5
        // 5. ???????????????????????????6
        // 6. ???????????????????????????????????????
        // 7 ???????????????????????????????????????
        mergeAudios();
    }

    private boolean isDuet = false;
    // ??????
    @Override
    public void duetShoot() {
        if (bgAudioDecoderFinish && videoCodecConfigResult){
            duet();
        } else{
            isDuet = true;
            // ???????????????
            outputViewOnSave();
        }
    }

    private void duet() {
        // ???????????????????????? record ??????????????????
        Intent intent = new Intent(this, RecordActivity.class);
        // ????????????????????????????????????????????????????????????????????????????????????
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
     * ????????????????????????
     * @param callback
     * @param audioPath
     * @param volume
     * @return
     */
    public String setValume(FFmpegCMD.OnExecCallback callback, String audioPath, float volume){
        final String newVolumeAudioPath = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".wav";
        String cmd = "ffmpeg -i " + String.format("\"%s\"",audioPath) + " -filter:a volume=" + volume + " -acodec pcm_s16le -ac 1 -ar 16000 " + String.format("\"%s\"",newVolumeAudioPath);
        Log.e(TAG, "ffmpeg ?????????????????????" + cmd);
        //FFmpegCMD.exec(cmd, callback);
        execFFmpegCmdSync(cmd, "FFmpeg set volume: ");
        callback.onExecuted(lastFFmpegCmdRet);
        return newVolumeAudioPath;
    }

    /**
     * 1. ???????????????????????????
     */
    private void mergeAudios() {
        // ???????????????????????????
        if (medias.size() > 1) {
            // ?????? ffmpeg ??????
            String audios = "";
            String filters = "";
            for (int i = 0; i < medias.size(); i++) {
                MediaInfoModel m = medias.get(i);
                audios += " -i " + String.format("\"%s\"",m.audioPath);
                filters += "[" + i + ":0]";
            }
            mergeAudioPath = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".wav";
            String cmd = "ffmpeg" + audios + " -filter_complex " + filters + "concat=n=" + medias.size() + ":v=0:a=1[out] -map [out] " + String.format("\"%s\"",mergeAudioPath);
            Log.e(TAG, "ffmpeg ?????????????????????" + cmd);
            execFFmpegCmdSync(cmd, "ffmpeg concatenate audio: ");
            if(lastFFmpegCmdRet == 0){
			    setOriginalValume();
			}
        } else {
            // ???????????????????????????
            mergeAudioPath = medias.get(0).audioPath;// ???????????????????????????
            setOriginalValume();
        }
    }

    /**
     * 2. ????????????
     */
    public void setOriginalValume(){
        if (originalValume > -1){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setOriginalVolumePath = setValume(new FFmpegCMD.OnExecCallback() {
                        @Override
                        public void onExecuted(int ret) {
                            Log.e(TAG, "?????????????????????" + ret);
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
     * 3. ???????????????????????????????????????
     */
    private void cutMusic() {
        if (music != null && TextUtils.isEmpty(music.cutPath)) {
            // ?????????????????????????????????????????????
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

                            Log.e(TAG, "FFmpeg ?????? : " + cmd);

                            execFFmpegCmdSync(cmd, "ffmpeg cut music: ");
                            if(lastFFmpegCmdRet == 0) {
                                music.cutPath = newAudioPath;
                                setMusicValume();
                            }
                            else
                                showToastInMainThread("??????????????????");
                        }

                        @Override
                        public void onError(Throwable e) {
                            showToastInMainThread("??????????????????");
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
     * 4. ??????????????????
     */
    public void setMusicValume(){
        if (musicVolume > -1 && music != null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setMusicVolumePath = setValume(new FFmpegCMD.OnExecCallback() {
                        @Override
                        public void onExecuted(int ret) {
                            Log.e(TAG, "?????????????????????" + ret);
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
     * 5. ???????????????????????????????????????
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
                    Log.e(TAG, "ffmpeg ?????????????????????" + cmd);
                    execFFmpegCmdSync(cmd, "FFmpeg combine music and audio: ");
                    if(lastFFmpegCmdRet == 0){
                        // ????????????
                        startMediaCodec();
                        // ????????????
                        startDecoder();
                    } else{
                        showToastInMainThread("??????????????????");
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
            // ????????????
            startMediaCodec();
            // ????????????
            startDecoder();
        }
    }

    /**
     * ?????????????????????
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
     * ???????????????????????????
     */
    private void startDecoder() {

        MediaInfoModel media = medias.get(0);

        // ??????????????????
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

        // ????????????????????????
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
     * ???????????????
     */
    private void startMediaCodec() {
        // ?????????????????????????????????

        String resolution = outputView.resolutionRadioGroup.getSelectedText();// ?????????
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
        // ??????????????????
//        int width = 1280; // ??????????????????????????????90???
//        int height = 720;
        int bitRate = 2 * 1024 * 1024; // ??????: 2Mbps 2 * 1024 * 1024
//        int bitRate = Integer.decode(videoBitrate) * 1000; // ??????: 2Mbps 2 * 1024 * 1024
        int fps = 30; // ??????: 30
        int iFrameInterval = 1; // GOP: 30

        // ??????????????????
        // Integer.decode(audioBitrate) * 1000
        int audioBitRate = 16 * 1000; // ??????: 128kbps
        int audioSampleRate = 16000; // ?????????: 44.1k

        // ???????????????
        MVYMediaCodecHelper.CodecInfo codecInfo = getAvcSupportedFormatInfo();
        if (codecInfo == null) {
            Log.d(TAG, "??????????????????");
            return;
        }

        // ???????????????????????????????????????????????????
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

        // ????????????
        mediaCodec = new MVYMediaCodec(outputMediaPath, 2);
        mediaCodec.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFill);
        videoCodecConfigResult = mediaCodec.configureVideoCodecAndStart(width, height, bitRate, fps, iFrameInterval, 1);
        audioCodecConfigResult = mediaCodec.configureAudioCodecAndStart(audioBitRate, audioSampleRate);

        Log.d(TAG, "?????????????????????????????????" + "width = " + width + "height = " + height + "bitRate = " + bitRate
                + "fps = " + fps + "IFrameInterval = " + iFrameInterval + "speedRate = " + 1 + videoCodecConfigResult + audioCodecConfigResult);
    }

    /**
     * ?????????????????????
     */
    private void addStickerSubtitle() {
        if (effectHandler == null) return;
        // ??????
//        String stickerPath = getIntent().getStringExtra(EditActivity.INTENT_DATA_STICKER);
//        String subtilePath = getIntent().getStringExtra(EditActivity.INTENT_DATA_SUBTTILE);
//        Log.e(TAG, "????????????????????????" + stickerPath + ", ?????????" + subtilePath);
//        if (!TextUtils.isEmpty(stickerPath) ){
//            try {
//                Bitmap stickerBitmap = BitmapFactory.decodeStream(getAssets().open(stickerPath));
//                MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerFilterModel = new MVYGPUImageStickerFilter.MVYGPUImageStickerModel(stickerBitmap);
//                effectHandler.addSticker(stickerFilterModel);
//
//                // ??????????????????
//                Matrix.rotateM(stickerFilterModel.transformMatrix, 0, 0.f, 0.f, 0.f, 1.f);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        if (!TextUtils.isEmpty(subtilePath)){
//            Bitmap bitmap = BitmapFactory.decodeFile(subtilePath);
//            MVYGPUImageStickerFilter.MVYGPUImageStickerModel subtitleModel = new MVYGPUImageStickerFilter.MVYGPUImageStickerModel(bitmap);
//            //  ??????????????????
//            Matrix.rotateM(subtitleModel.transformMatrix, 0, 0.f, 0.f, 0.f, 1.f);
//            Matrix.translateM(subtitleModel.transformMatrix, 0, 0.f, -0.6f, 0.0f);
//            effectHandler.addSticker(subtitleModel);
//        }
    }

    /**
     * ?????????????????????
     */
    private void stopMediaAndCodec() {
        if (mediaCodec != null){
            mediaCodec.finish();
        }

        // ??????????????????
        if (originalBgAudioDecoder != null){
            originalBgAudioDecoder.stopDecoder();
            originalBgAudioDecoder.stopPlayer();
        }

    }

    /**
     * ???????????????????????????
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
     * ?????????????????????????????????
     */
    private void synthesizeMedia() {
        // mediaCodec ???????????????????????????????????????
        if (videoDecoderFinish && bgAudioDecoderFinish && mediaCodec != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // ??????dialog
                    alertDialog.dismiss();
                    stopMediaAndCodec();
                    destroyDecoder();

                    for (MediaInfoModel m : medias) {
                        Log.e(TAG, "????????????" + m.videoPath + ", ?????????"+ m.audioPath);
                    }
                    Log.e(TAG, "????????????, combineMusicPath:" + mergeAudioPath + ", combineMusicPath:" + combineMusicPath);
                    Log.e(TAG, "???????????????" + outputMediaPath);
                    String gifPath = outputMediaPath + "_short.gif";
                    String cmd = "ffmpeg -ss 0 -t 2 -i " + String.format("\"%s\"",outputMediaPath) + " -vf \"fps=5,scale=320:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse\" " + String.format("\"%s\"",gifPath);
                    Log.e(TAG, "ffmpeg ?????????????????????" + cmd);
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
     * ??????????????????????????????????????????
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
            Toast.makeText(OutputActivity.this, "???????????????" + outputMediaPath, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * ??????????????????
     */
    private void combineDuetAndMedia() {
        if (!TextUtils.isEmpty(duetPath)){
            alertDialog = new AlertDialog.Builder(this).setMessage("????????????????????????").create();
            alertDialog.setCancelable(false);
            alertDialog.show();
            combinedDuetPath = FFmpegManager.executeCombieMultipleVideo(medias.get(0).videoPath, duetPath,width, height, new FFmpegCMD.OnExecCallback() {
                @Override
                public void onExecuted(int ret) {
                    Log.e(TAG, "???????????????????????????" + ret + " , path=" + combinedDuetPath);
                    parseCombineDuetResult(ret);
                }
            });
            return;
        }
    }

    /**
     * ?????????????????????????????????????????????
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
                    Toast.makeText(OutputActivity.this, "????????????", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * ???????????????????????????
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
//        Log.e(TAG, "????????????????????????" + newPath);
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
     * ??????video???mine_type,??????mp4,3gp
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
