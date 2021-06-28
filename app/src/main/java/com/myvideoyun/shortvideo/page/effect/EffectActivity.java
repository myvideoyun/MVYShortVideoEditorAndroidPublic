package com.myvideoyun.shortvideo.page.effect;

import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;

import com.myvideoyun.decoder.FFmpegCMD;
import com.myvideoyun.decoder.VideoDecoder;
import com.myvideoyun.decoder.VideoFrame;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageShortVideoFilter;
import com.myvideoyun.shortvideo.MVYVideoEffectHandler;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.page.common.EffectHorizontalView;
import com.myvideoyun.shortvideo.page.cover.CoverSelectionViewCallback;
import com.myvideoyun.shortvideo.page.effect.model.Effect;
import com.myvideoyun.shortvideo.page.effect.model.EffectModel;
import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;
import com.myvideoyun.shortvideo.MVYPreviewView;
import com.myvideoyun.shortvideo.tools.ScreenTools;

import java.util.ArrayList;
import java.util.UUID;

import static java.lang.Thread.sleep;

/**
 * Created by 汪洋 on 2019/3/29.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class EffectActivity extends AppCompatActivity
        implements SurfaceHolder.Callback
        , CoverSelectionViewCallback
//        , VideoAccurateSeekDecoder.VideoAccurateSeekDecoderListener
        , EffectHorizontalView.StylePlaneOnClickItemListener, VideoDecoder.VideoPlaybackListener {

    public static final String RESULT_DATA = "music_result_data";
    private static final String TAG = "EffectActivity";
    public static final String DATA_COVER_BITMAP_PATH = "cover_bitmap_path";
    EffectView effetView;

    // 预览
    MVYPreviewView surfaceView;

    // 数据处理
    MVYVideoEffectHandler effectHandler;

    // 解码
//    VideoAccurateSeekDecoder videoDecoder;
    // 连续播放解码器
    VideoDecoder videoDecoder2;

    // 是否是倒序解码
    private boolean isReverseDecode;
    private ArrayList<MediaInfoModel> medias;
    //    private MediaInfoModel mVideo;
    private float mTotalDuration;
//    private Bitmap mCoverBitmap;
    // 当前最新的一帧
    private VideoFrame currentFrame;
    private String rotation;
    // 当前点击的特效
    private Effect tempEffect;
    private EffectModel effectModel;

    private String reverseVideoPath = "";

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

        effetView = new EffectView(getBaseContext());
        surfaceView = effetView.previewView;
        surfaceView.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFit);
        surfaceView.getHolder().addCallback(this);
        effetView.callback = this;
        effetView.effetListView.setOnClickItemListener(this);
        setContentView(effetView);

        medias = (ArrayList<MediaInfoModel>) getIntent().getSerializableExtra("medias");
        Log.d(TAG, "需要裁剪的视频：" + medias.toString());

        String[] paths = new String[medias.size()];
        MediaInfoModel tempM;
        for (int i = 0; i < medias.size(); i++) {
            tempM = medias.get(i);
            mTotalDuration += tempM.videoSeconds;
            paths[i] = tempM.videoPath;
        }
        // 预览视频宽高
        setPreviewRatio();

        float minLength = mTotalDuration / 15;
        Log.d(TAG, "设置裁剪框 max：" + mTotalDuration + ", min:" + minLength);

        effetView.rangeView.setMaxLength(mTotalDuration);
        effetView.rangeView.setMinLength(minLength);

//        videoDecoder = new VideoAccurateSeekDecoder();
        videoDecoder2 = new VideoDecoder(true);
//        videoDecoder.setDecoderListener(this);
        videoDecoder2.setPlaybackListener(this);

        // 创建本地解码器
//        videoDecoder.createNativeVideoDecoder(paths);
        videoDecoder2.createNativeVideoDecoder(paths);
//        videoDecoder2.startDecodeFromFirstFrame(0);

    }

    private void resetToDefaultVideoFile(){
        String[] paths = new String[medias.size()];
        MediaInfoModel tempM;
        for (int i = 0; i < medias.size(); i++) {
            tempM = medias.get(i);
            mTotalDuration += tempM.videoSeconds;
            paths[i] = tempM.videoPath;
        }
        if(videoDecoder2 != null) {
            videoDecoder2.stopDecoder();
            videoDecoder2.destroyNativeVideoDecoder();
        }
        videoDecoder2.createNativeVideoDecoder(paths);
    }

    /**
     * 设置预览 view 的宽高
     */
    private void setPreviewRatio() {
        // 设置surface view 宽高
        Point screen = ScreenTools.getScreen(this);
        MediaInfoModel video = medias.get(0);
        if (video.height > 0 && video.width >0) {
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(0, 0);
            params.height = (int) (screen.x * video.height / video.width);
            params.width = screen.x;
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
            params.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;

            // 获取视频方向
            MediaMetadataRetriever retr = new MediaMetadataRetriever();
            retr.setDataSource(video.videoPath);
            rotation = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            Log.d(TAG, "当前视频的 rotation:" + rotation);
            if ("90".equals(rotation) || "270".equals(rotation)) {
//            params.width = screen.x;
                params.height = (int) (screen.x * video.width / video.height);
            }
            surfaceView.setLayoutParams(params);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
//        // 正序解码
//        videoDecoder.startAccurateSeekDecode();
//        videoDecoder.setSeekTime(0);
        isReverseDecode = false;
    }

    @Override
    protected void onStop() {
        stopDecoder();
        super.onStop();
    }

    /**
     * 停止解码器
     */
    private void stopDecoder() {
//        if (videoDecoder != null) {
//            videoDecoder.stopDecoder();
//        }
        if (videoDecoder2 != null) {
            videoDecoder2.stopDecoder();
//            videoDecoder2.stopPlayer();
        }
    }

    @Override
    protected void onDestroy() {
        // 关闭解码器
        if (videoDecoder2 != null) {
            videoDecoder2.destroyNativeVideoDecoder();
            videoDecoder2 = null;
        }
//        if (videoDecoder != null) {
//            videoDecoder.destroyNativeVideoDecoder();
//            videoDecoder = null;
//        }
        super.onDestroy();
    }

    @Override
    public void finish() {
        stopDecoder();
        super.finish();
    }

    @Override
    public void onBackPressed() {
        stopDecoder();
        super.onBackPressed();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        effectHandler = new MVYVideoEffectHandler(getApplicationContext());
        effetView.rangeView.addAllSlideRect(EffectRestore.effects);
        if (EffectRestore.effects.size() > 0){
            effetView.withdrawBtn.setVisibility(View.VISIBLE);
        }

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
    public void backwards() {
        finish();
    }

    @Override
    public void finishEdit() {
        finish();
    }

    @Override
    public void cutChoiseStart(float position) {
    }

    @Override
    public void cutFinish(float start, float end) {
//        Log.e(TAG, "选中的范围，start=" + start + "， end=" + end);
        setDecoderRange(start, end);
    }

    @Override
    public void dragging(float start, float end) {
        setDecoderRange(start, end);
        // 控制每次开始解码到结束，只渲染一帧到 surfaceView
    }

    /**
     * 设置 decoder 预览范围
     * @param start
     * @param end
     */
    private void setDecoderRange(float start, float end) {
//        if (videoDecoder != null) videoDecoder.setSeekTime(start);
//        if (videoDecoder2 != null) {
//            if (videoDecoder2.isPlaying() || videoDecoder2.isDecoding()){
//                videoDecoder2.stopPlayer();
//                videoDecoder2.stopDecoder();
//            }
//            if (EffectRestore.isReverse){
//                videoDecoder2.startDecodeFromLastFrame(start);
//            } else {
//                videoDecoder2.startDecodeFromFirstFrame(start);
//            }
//        }
    }

    @Override
    public void playbackVideoOutput(VideoDecoder decoder, final VideoFrame frame) {
        // 渲染yuv数据到纹理
        if (effectHandler != null) {
            EffectRestore.setEffectHandlerType(effectHandler, frame, tempEffect);
            int texture;
            if (frame.rotate == 90) {
                texture = effectHandler.processWithYUVData(frame.yData, frame.uData, frame.vData, frame.width, frame.height, frame.lineSize, MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateRight);
            } else if (frame.rotate == 270) {
                texture = effectHandler.processWithYUVData(frame.yData, frame.uData, frame.vData, frame.width, frame.height, frame.lineSize, MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateLeft);
            } else {
                texture = effectHandler.processWithYUVData(frame.yData, frame.uData, frame.vData, frame.width, frame.height, frame.lineSize, MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageNoRotation);
            }
            // 渲染到surfaceView
            if (surfaceView != null) {
                if (frame.rotate == 90 || frame.rotate == 270) {
                    surfaceView.render(texture, frame.height, frame.width);
                } else {
                    surfaceView.render(texture, frame.width, frame.height);
                }
            }
            //
            if (effectModel != null && effectModel.effectType != 0){
                effetView.rangeView.setProgress((float) frame.pts);
            }
        }
    }

    @Override
    public void playbackStop(VideoDecoder decoder) {
        Log.w("moose", "playbackStop");
    }

    @Override
    public void playbackFinish(VideoDecoder decoder) {
        Log.w("moose", "playbackFinish");
    }

    // ============= 特效选择回调 start ==============
//    private long startTime;
    // generate the backplayed video;
    private  void generateReverseVideo(){
        reverseVideoPath = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".mp4";

        String cmd = "ffmpeg -i " + String.format("\"%s\"",medias.get(0).videoPath) + " -vf reverse -c:v libx264 -preset ultrafast -crf 22 " + String.format("\"%s\"", reverseVideoPath);
        Log.e(TAG, "ffmpeg reverse video：" + cmd);
        execFFmpegCmdSync(cmd, "Reverse the video");
        String [] paths = new String[1];
        paths[0] = reverseVideoPath;
        if(videoDecoder2 != null){
            videoDecoder2.stopDecoder();
            videoDecoder2.destroyNativeVideoDecoder();
        }
        videoDecoder2.createNativeVideoDecoder(paths);
    }

    @Override
    public void onClickItem(int position, EffectModel model) {
        Log.e("moose", "onClickItem===" + position);
        if (model.effectType != 0){
            this.effectModel = model;
            videoDecoder2.stopDecoder();
            videoDecoder2.stopPlayer();
            EffectRestore.isNormal = false;
            EffectRestore.isReverse = false;
            EffectRestore.isFast = false;
            EffectRestore.isSlow = false;
            if(position != 1 && EffectRestore.reverseVideoPaths != ""){
                // previous effects is reverse play, now we need reset it back
                resetToDefaultVideoFile();
                EffectRestore.reverseVideoPaths = "";
            }

            switch (position) {
                case 0: {
                    EffectRestore.isNormal = true;
                    effetView.rangeView.setProgress(0);
                    videoDecoder2.startDecodeFromFirstFrame();
                    break;
                }
                case 1: {
                    Log.e("moose", "Reverse Playback");
                    if(reverseVideoPath == "")
                        generateReverseVideo();
                    EffectRestore.isReverse = true;
                    EffectRestore.reverseVideoPaths = reverseVideoPath;
                    effetView.rangeView.setProgress(effetView.rangeView.getMaxLength() - effetView.rangeView.getMinLength());
                    videoDecoder2.startDecodeFromFirstFrame();
                    break;
                }
                case 2: {
                    Log.e("moose", "fast Playback");
                    EffectRestore.isFast = true;
                    effetView.rangeView.setProgress(0);
                    videoDecoder2.startFastDecoding();
                    break;
                }
                case 3: {
                    Log.e("moose", "slow Playback");
                    EffectRestore.isSlow = true;
                    effetView.rangeView.setProgress(0);
                    videoDecoder2.startSlowDecoding();
                    break;
                }
                default: break;

            }
            videoDecoder2.startPlayer();
        } else {
            this.effectModel = null;
        }
    }

    @Override
    public void onTouchingItem(int position, long time, EffectModel model) {
        if (model.effectType == 0) {
            // 根据视频播放方向正序或者反序操作进度条
            if (EffectRestore.isReverse){
                effetView.rangeView.addProgressFromLast(100, model);
            } else {
                effetView.rangeView.addProgress(100, model);
            }
        }
    }

    @Override
    public void onStartTouch(int position, long time, EffectModel model) {
        if (model.effectType == 0) {
            this.effectModel = null;
            if ((effetView.rangeView.getEndProgress() < effetView.rangeView.getMaxLength() && !EffectRestore.isReverse)
                || (effetView.rangeView.getStartProgress() > 0 && EffectRestore.isReverse)) {
                Log.e("moose", "onStartTouch");
                videoDecoder2.stopDecoder();
                videoDecoder2.stopPlayer();
                if (!EffectRestore.isReverse) {
                    videoDecoder2.startDecodeFromFirstFrame((int)effetView.rangeView.getStartProgress(), 1.0f);
                } else {
                    float endProgress = (int)(effetView.rangeView.getMaxLength() - effetView.rangeView.getEndProgress());
                    videoDecoder2.startDecodeFromLastFrame(endProgress);
                    Log.e("moose", "反序播放进度：" + endProgress + ", 实际长度：" + mTotalDuration);
                }
                videoDecoder2.startPlayer();
                if (effetView.rangeView.getEndProgress() < mTotalDuration) {
                    // rangview 没有拖动到最后，创建临时 effect，获取开始时间
                    tempEffect = new Effect();
                    tempEffect.model = model;
                    tempEffect.startTime = effetView.rangeView.getStartProgress();
                }
                Log.e(TAG, "视频开始时间：" + effetView.rangeView.getStartProgress());
                if (!EffectRestore.isReverse) {
                    effetView.rangeView.addSlideRect(model);
                } else {
                    effetView.rangeView.addSlideRectFromLast(model);
                }
                effetView.withdrawBtn.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onEndTouched(int position, long time, EffectModel model) {
        if (model.effectType == 0) {
            Log.e("moose", "onEndTouched");
            // 停止播放
            videoDecoder2.stopDecoder();
            videoDecoder2.stopPlayer();

            // 按住当前特效的时间长度
            float endProgress = effetView.rangeView.getEndProgress();
            float startProgress = effetView.rangeView.getStartProgress();
//            Log.e(TAG, "视频结束时间：" + endProgress);
            effetView.rangeView.clearCurrentSlideRect();
            // 保存特效到集合
            if (tempEffect != null) {
                if (!EffectRestore.isReverse) {
                    tempEffect.duration = startProgress - tempEffect.startTime;
                    if (endProgress >= mTotalDuration) {
                        // 滑到结束为止了
                        tempEffect.duration = endProgress - tempEffect.startTime;
                    }
                } else {
                    if (startProgress <= 0) {
                        // 滑到开始
                        tempEffect.duration = tempEffect.startTime - startProgress;
                        tempEffect.startTime = startProgress;
                    } else {
                        tempEffect.duration = tempEffect.startTime - endProgress;
                        tempEffect.startTime = endProgress;
                    }
                }
                tempEffect.model = model;
                EffectRestore.effects.add(tempEffect);
                tempEffect = null;
            }
            Log.e("moose", "当前选择的特效：" + EffectRestore.effects);
        }
    }
    // ============= 特效选择回调 end ==============

}