package com.myvideoyun.shortvideo.page.cover;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.myvideoyun.decoder.VideoAccurateSeekDecoder;
import com.myvideoyun.decoder.VideoFrame;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageStickerFilter;
import com.myvideoyun.shortvideo.MVYVideoEffectHandler;
import com.myvideoyun.shortvideo.page.effect.EffectRestore;
import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;
import com.myvideoyun.shortvideo.page.record.model.StyleModel;
import com.myvideoyun.shortvideo.MVYPreviewView;
import com.myvideoyun.shortvideo.tools.FileTools;
import com.myvideoyun.shortvideo.tools.ScreenTools;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by yangshunfa on 2019/3/9.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class CoverSelectionActivity extends AppCompatActivity
        implements SurfaceHolder.Callback
        , CoverSelectionViewCallback, VideoAccurateSeekDecoder.VideoAccurateSeekDecoderListener {

    public static final String RESULT_DATA = "music_result_data";
    private static final String TAG = "CoverSelectionActivity";
    public static final String DATA_COVER_BITMAP_PATH = "cover_bitmap_path";
    CoverSelectionView coverView;

    // 预览
    MVYPreviewView surfaceView;

    // 数据处理
    MVYVideoEffectHandler effectHandler;

    // 解码
    VideoAccurateSeekDecoder videoDecoder;
    // 贴纸
    private ArrayList<StyleModel> stickerModels = new ArrayList<>();

    // 是否是倒序解码
    private boolean isReverseDecode;
    private ArrayList<MediaInfoModel> medias;
    //    private MediaInfoModel mVideo;
    private float mTotalDuration;
    private StyleModel mStickerModel;
    private MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerFilterModel;
    private Bitmap mCoverBitmap;
    // 当前最新的一帧
    private VideoFrame currentFrame;
    private String rotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        coverView = new CoverSelectionView(getBaseContext());
        surfaceView = coverView.previewView;
        surfaceView.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFit);
        surfaceView.getHolder().addCallback(this);
        coverView.callback = this;
        setContentView(coverView);

        medias = (ArrayList<MediaInfoModel>) getIntent().getSerializableExtra("medias");
        Log.d(TAG, "需要裁剪的视频：" + medias.toString());

        String[] paths = new String[medias.size()];
        MediaInfoModel tempM;
        for (int i = 0; i < medias.size(); i++) {
            tempM = medias.get(i);
            mTotalDuration += tempM.videoSeconds;
            paths[i] = tempM.videoPath;
        }

        // 设置surface view 宽高
        Point screen = ScreenTools.getScreen(this);
        MediaInfoModel mVideo = medias.get(0);
        if (mVideo.height > 0 && mVideo.width >0) {
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(0, 0);
            params.height = (int) (screen.x * mVideo.height / mVideo.width);
            params.width = screen.x;
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
            params.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;

            // 获取视频方向
            MediaMetadataRetriever retr = new MediaMetadataRetriever();
            retr.setDataSource(mVideo.videoPath);
            rotation = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            Log.d(TAG, "当前视频的 rotation:" + rotation);
            if ("90".equals(rotation) || "270".equals(rotation)) {
//            params.width = screen.x;
                params.height = (int) (screen.x * mVideo.width / mVideo.height);
            }
            surfaceView.setLayoutParams(params);
        }

        float minLength = mTotalDuration / 15;
        Log.d(TAG, "设置裁剪框 max：" + mTotalDuration + ", min:" + minLength);

        coverView.rangeView.setMaxLength(mTotalDuration);
        coverView.rangeView.setMinLength(minLength);

        videoDecoder = new VideoAccurateSeekDecoder();
        videoDecoder.setDecoderListener(this);

        // 创建本地解码器
        videoDecoder.createNativeVideoDecoder(paths);

    }

    @Override
    protected void onStart() {
        super.onStart();
//        // 正序解码
        videoDecoder.startAccurateSeekDecode();
        videoDecoder.setSeekTime(0);
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
        if (videoDecoder != null) {
            videoDecoder.stopDecoder();
        }
    }

    @Override
    protected void onDestroy() {
        // 关闭解码器
        if (videoDecoder != null) {
            videoDecoder.destroyNativeVideoDecoder();
            videoDecoder = null;
        }
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
        // 1. 将当前 currentFrame 转换成 bitmap并存入 cache

        if (currentFrame != null) {
            if (currentFrame.rotate == 90 || currentFrame.rotate == 270) {
                mCoverBitmap = effectHandler.getCurrentImage(currentFrame.height, currentFrame.width);
            } else {
                mCoverBitmap = effectHandler.getCurrentImage(currentFrame.width, currentFrame.height);
            }
        }
//        coverView.coverIv.setImageBitmap(mCoverBitmap);
        // 2. 下一步，将bitmap存到缓存，回调path
        if (mCoverBitmap != null) {
            String path = FileTools.saveBitmapToCache(getExternalCacheDir().getAbsolutePath(), UUID.randomUUID().toString().replace("-", "") + ".jpeg", mCoverBitmap);
            if (!TextUtils.isEmpty(path)) {
                Intent data = new Intent();
                data.putExtra(DATA_COVER_BITMAP_PATH, path);
                setResult(RESULT_OK, data);
                Toast.makeText(this, "已选定封面", Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    @Override
    public void cutChoiseStart(float position) {
    }

    @Override
    public void cutFinish(float start, float end) {
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
        Log.d(TAG, "cut video finish: start=" + start + " end=" + end);
        // 把上一个获取的封面bitmap置空
        if (mCoverBitmap != null) {
//            coverView.coverIv.setImageBitmap(null);
            mCoverBitmap.recycle();
            mCoverBitmap = null;
        }
        videoDecoder.setSeekTime(start);
    }

    @Override
    public void seekToFrameUpdate(VideoAccurateSeekDecoder decoder, final VideoFrame frame) {
        // 切换到主线程渲染
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 渲染yuv数据到纹理
                if (effectHandler != null) {
                    int texture;
                    EffectRestore.setEffectHandlerType(effectHandler, frame, null);
                    if (frame.rotate == 90) {
                        texture = effectHandler.processWithYUVData(frame.yData, frame.uData, frame.vData, frame.width, frame.height, frame.lineSize, MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateRight);
                    } else if (frame.rotate == 270) {
                        texture = effectHandler.processWithYUVData(frame.yData, frame.uData, frame.vData, frame.width, frame.height, frame.lineSize, MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateLeft);
                    } else {
                        texture = effectHandler.processWithYUVData(frame.yData, frame.uData, frame.vData, frame.width, frame.height, frame.lineSize, MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageNoRotation);
                    }
                    // 缓存当前帧，返回时作为截图
                    CoverSelectionActivity.this.currentFrame = frame;
                    // 渲染到surfaceView
                    if (surfaceView != null) {
                        if (frame.rotate == 90 || frame.rotate == 270) {
                            surfaceView.render(texture, frame.height, frame.width);
                        } else {
                            surfaceView.render(texture, frame.width, frame.height);
                        }
                    }
                }
            }
        });

    }

    @Override
    public void decoderStop(VideoAccurateSeekDecoder decoder) {

    }
}