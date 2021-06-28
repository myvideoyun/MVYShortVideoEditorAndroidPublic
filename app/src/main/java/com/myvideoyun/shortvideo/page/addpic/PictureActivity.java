package com.myvideoyun.shortvideo.page.addpic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;

import com.myvideoyun.decoder.VideoDecoder;
import com.myvideoyun.decoder.VideoFrame;
import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.customUI.StickerView;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageStickerFilter;
import com.myvideoyun.shortvideo.MVYVideoEffectHandler;
import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;
import com.myvideoyun.shortvideo.page.record.model.StyleModel;
import com.myvideoyun.shortvideo.MVYPreviewView;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yangshunfa on 2019/3/9.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class PictureActivity extends AppCompatActivity
        implements SurfaceHolder.Callback, VideoDecoder.VideoDecoderListener
        , VideoDecoder.VideoPlaybackListener
    , PictureViewCallback {

    public static final String RESULT_DATA = "music_result_data";
    private static final String TAG = "PictureActivity";
    PictureView pictureView;

    // 预览
    MVYPreviewView surfaceView;

    // 数据处理
    MVYVideoEffectHandler effectHandler;

    // 解码
    VideoDecoder videoDecoder;
    // 贴纸
    private ArrayList<StyleModel> stickerModels = new ArrayList<>();

//    private String iFrameVideoPath;
//    private String videoPath;
//    private String audioPath;

    // 是否是倒序解码
    private boolean isReverseDecode;
//    private MediaPlayer mediaPlayer;
    private ArrayList<MediaInfoModel> medias;
    private MediaInfoModel mVideo;
    private float mTotalDuration;
//    private MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerModel;
    private float mEnd;
    private float mStart;
    private StyleModel mStickerModel;
    private MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerFilterModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pictureView = new PictureView(getBaseContext());
//        pictureView.callback = this;
        surfaceView = pictureView.previewView;
        surfaceView.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFit);
        surfaceView.getHolder().addCallback(this);
        setContentView(pictureView);
        pictureView.callback = this;
        medias = (ArrayList<MediaInfoModel>) getIntent().getSerializableExtra("medias");
        mVideo = medias.get(0);
        Log.d(TAG, "需要裁剪的视频：" + medias.toString());
        String[] paths = new String[medias.size()];
        MediaInfoModel tempM;
        for (int i= 0 ; i< medias.size() ;i++){
            tempM = medias.get(i);
            mTotalDuration += tempM.videoSeconds;
            paths[i] = tempM.videoPath;
        }

        pictureView.mRangeView.setMaxLength(mTotalDuration);
        float minLength = mTotalDuration / 15;
        pictureView.mRangeView.setMinLength(minLength);
        Log.d(TAG, "设置裁剪框 max：" + mTotalDuration + ", min:" + minLength);
        mStart = 0;
        mEnd = mTotalDuration;

//        mediaPlayer = new MediaPlayer();
//        try {
//            mediaPlayer.setDataSource(mVideo.audioPath);
//            mediaPlayer.prepare();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        videoDecoder = new VideoDecoder(true);
        videoDecoder.setDecoderListener(this);
        videoDecoder.setPlaybackListener(this);

        // 创建本地解码器
        videoDecoder.createNativeVideoDecoder(paths);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // 正序解码
        videoDecoder.startDecodeFromFirstFrame();
        isReverseDecode = false;
        // 倒序解码
//        videoDecoder.startDecodeFromLastFrame(1.5f);

        // 开始播放
//        videoDecoder.startDecodeFromFirstFrame();
//        mediaPlayer.start();
    }

    @Override
    protected void onDestroy() {
        // 停止音乐播放器
//        mediaPlayer.stop();
//        mediaPlayer.release();
//        mediaPlayer = null;

        // 停止解码
        videoDecoder.stopDecoder();

        // 关闭解码器
        videoDecoder.destroyNativeVideoDecoder();
        videoDecoder = null;

        super.onDestroy();
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
//        Log.d(TAG, "decoderStop");
    }

    @Override
    public void decoderFinish(VideoDecoder decoder) {
//        Log.d(TAG, "decoderFinish");
    }

    @Override
    public void playbackVideoOutput(VideoDecoder decoder, final VideoFrame frame) {

        // 渲染yuv数据到纹理
        if (effectHandler != null) {

            if (frame.pts >= mStart && frame.pts <= mEnd){
//                        effectHandler.addSticker(stickerModel);
                addSticker(mStickerModel);
            } else {
                removeSticker();
            }

            int texture;
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
//        Log.d(TAG, "playbackStop");
    }

    @Override
    public void playbackFinish(VideoDecoder decoder) {
//        Log.d(TAG, "playbackFinish");

        //----------测试连续播放----------
//        if (isReverseDecode) {
//            // 正序解码
            videoDecoder.startDecodeFromFirstFrame();
//        } else {
//            // 倒序解码
//            videoDecoder.startDecodeFromLastFrame();
//        }
//
//        isReverseDecode = !isReverseDecode;
        //----------测试连续播放----------

    }

    @Override
    public void backwards() {
        finish();
    }

    @Override
    public void finishEdit() {
//        Intent data = new Intent(this, EditActivity.class);
//        data.putExtra("medias", medias);
//        startActivity(data);
//        finish();
        // finish
    }

    @Override
    public void cutChoiseStartPosition(float position) {
    }

    @Override
    public void cutChoiseEndPosition(float position) {
    }

    @Override
    public void cutFinish(float start, float end) {
//        mVideo.start =start;
//        mVideo.end = end;
        this.mStart = start;
        this.mEnd = end;
        double second =  Math.floor(start / 1000);
        double lastSEcond = Math.floor(end / 1000);
        Log.d(TAG, "cut video finish: start=" + start + " end=" + end + " second = " + second + " lastSecond = " + lastSEcond);
        videoDecoder.startDecodeFromFirstFrame();
//        videoDecoder.startDecodeFromLastFrame();
//        videoDecoder.seekTo(start/1000 + start % 1000);
//        mediaPlayer.seekTo((int) start);
//        mediaPlayer.start();
    }

    @Override
    public void cutChoosing() {
        videoDecoder.stopDecoder();
//        mediaPlayer.pause();
    }

    @Override
    public void addStickerToVideo(StyleModel model) {
        if (stickerModels.contains(model)){
            StyleModel styleModel = new StyleModel();
            styleModel.path = model.path;
            styleModel.thumbnail = model.thumbnail;
            styleModel.text = model.text;
            addSticker(styleModel);
        } else {
            addSticker(model);
        }
    }

    /**
     * 从effectHandler 中移除贴纸
     */
    private void removeSticker() {
        if (mStickerModel == null) return;
        if (stickerModels.contains(mStickerModel)){
            Log.d("moose", "removeSticker " + mStickerModel);
            effectHandler.removeSticker(stickerFilterModel);
            stickerModels.remove(mStickerModel);
            stickerFilterModel = null;
        }
    }

    /**
     * 添加贴纸到视频中
     * @param model
     */
    private void addSticker(StyleModel model) {
        if (model == null) return;
        if (stickerModels.contains(model)){
            return;
        }
        Log.d("moose", "addStickerToVideo " + model);

        this.mStickerModel = model;
        // 添加贴纸
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getAssets().open(model.thumbnail));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bitmap != null) {
            stickerFilterModel = new MVYGPUImageStickerFilter.MVYGPUImageStickerModel(bitmap);
            effectHandler.addSticker(stickerFilterModel);
            stickerModels.add(model);
            StickerView stickerView = new StickerView(this);
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(100, 100);
            params.topToTop = R.id.previewRootView;
            params.bottomToBottom = R.id.previewRootView;
            params.rightToRight = R.id.previewRootView;
            params.leftToLeft = R.id.previewRootView;
            pictureView.previewRootView.addView(stickerView, params);

            // todo 设置贴纸位置
//            Matrix.rotateM(stickerFilterModel.transformMatrix, 0, 10.f, 0.f, 0.f, 1.f);
//            Matrix.translateM(stickerFilterModel.transformMatrix, 0, 0.5f, 0f, 0.0f);
//            Matrix.translateM(stickerFilterModel.transformMatrix, 0, 0.8f, -0.5f, 0.0f);
//            Matrix.translateM(stickerFilterModel.transformMatrix, 0, -0.5f, 0.8f, 0.0f);
        }
    }
}