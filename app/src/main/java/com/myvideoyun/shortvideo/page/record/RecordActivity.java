package com.myvideoyun.shortvideo.page.record;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.myvideoyun.decoder.VideoDecoder;
import com.myvideoyun.decoder.VideoFrame;
import com.myvideoyun.shortvideo.Constant;
import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.MVYCameraEffectHandler;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.page.edit.EditActivity;
import com.myvideoyun.shortvideo.page.input.video.ImportVideoActivity;
import com.myvideoyun.shortvideo.page.input.InputActivity;
import com.myvideoyun.shortvideo.page.music.MusicActivity;
import com.myvideoyun.shortvideo.page.music.model.MediaModel;
import com.myvideoyun.shortvideo.page.output.OutputActivity;
import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;
import com.myvideoyun.shortvideo.page.record.model.StyleModel;
import com.myvideoyun.shortvideo.recordTool.MVYAudioRecordListener;
import com.myvideoyun.shortvideo.recordTool.MVYAudioRecordWrap;
import com.myvideoyun.shortvideo.recordTool.MVYCameraPreviewListener;
import com.myvideoyun.shortvideo.recordTool.MVYCameraPreviewWrap;
import com.myvideoyun.shortvideo.recordTool.MVYMediaCodec;
import com.myvideoyun.shortvideo.recordTool.MVYMediaCodecHelper.CodecInfo;
import com.myvideoyun.shortvideo.MVYPreviewView;
import com.myvideoyun.shortvideo.recordTool.MVYWavAudioRecord;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageStickerFilter;
import com.myvideoyun.shortvideo.tools.ScreenTools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import static com.myvideoyun.shortvideo.recordTool.MVYMediaCodecHelper.getAvcSupportedFormatInfo;

/**
 * Created by ?????? on 2019/2/2.
 * Copyright ?? 2019??? myvideoyun. All rights reserved.
 */
public class RecordActivity extends AppCompatActivity implements
        RecordViewCallback, SurfaceHolder.Callback, MVYCameraPreviewListener, MVYAudioRecordListener,
        VideoDecoder.VideoPlaybackListener, MediaPlayer.OnPreparedListener {

    private static final String TAG = "RecordActivity";
    public static final String INTENT_AUDIO_PATH = "audioPath";

    private static final int REQUEST_CODE_CHOOSE_MUSIC = 100;
    private static final int REQUEST_CODE_CHOOSE_DUET = 101;

    // ????????????
    int CHECK_PERMISSION_REQUEST_CODE = 1000;


    // ?????????????????????????????????
    String resolution = "720p";

    // ????????????????????????????????????
    String frameRate = "30";

    // ????????????????????????????????????
    String videoBitrate = "2048";

    // ????????????????????????????????????
    String audioBitrate = "8";

    private float mMaxRecordTime = 15 * 1000;// 30 000??????

    // ????????????????????????????????????
    private long mRecordedTotalDuration = 0;
    // ?????????????????????????????????????????????
    private long lastRecordDuration = 0;

    /// ??????????????????
    private long mLastTimeStamp = 0;

    // view
    RecordView recordView;

    // ??????
    MediaInfoModel recordingMedia = new MediaInfoModel();
    ArrayList<MediaInfoModel> medias = new ArrayList<>();
    // ???????????????
    private String duetMediaPath;
//    private ArrayList<MediaInfoModel> duetMediaPath;

    // ??????
    Camera camera;
    MVYCameraPreviewWrap cameraPreviewWrap;

    // ?????????
    AudioRecord audioRecord;
    MVYAudioRecordWrap audioRecordWrap;

    // ????????????
    MVYCameraEffectHandler effectHandler;

    // ?????????surface
    MVYPreviewView surfaceView;
    // ?????????surface
    SurfaceView duetSurfaceView;

    // ??????????????????
    MVYMediaCodec videoCodec;
    boolean videoCodecConfigResult = false;

    // AudioRecord is accessed from two thread, so it should
    // be protected using lock.
    private Lock audioRecordLock = new ReentrantLock();

    // ?????????????????????
//    VideoDecoder duetVideoDecoder;

    // ????????????????????????
    public static final int audioSampleRate = 16000;   //???????????????
    public static final int audioChannel = AudioFormat.CHANNEL_IN_MONO;   //?????????
    public static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT; //??????????????????

    // wav??????????????????
    MVYWavAudioRecord wavAudioRecord;
    boolean isWavAudioRecord = false;

    private MediaPlayer player;

    public static final int FRONT_CAMERA_ID = 1;
    public static final int BACK_CAMERA_ID = 0;
    private int mCurrentCameraID = FRONT_CAMERA_ID;

    // ????????????
    private int mWidth = 1280;
    private int mHeight = 720;
    private int mFrameRate = 30;
    private int mVideoBitrate = 20000000;// default 2Mbps
    private int mAudioBitrate = 128000;// default 128Kbps
    private double previewRatio = 0.5652;// 9/16 16:9
    private float mSpeedRate = 1f;

    private MediaPlayer mediaPlayer;
    // ???????????????
    private MediaModel mMusic;
    // ???????????????
    private Bitmap picture;
//    private MultileMediaPlayer multileMediaPlayer;
    private MediaPlayer duetMediaPlayer;
    private String path;
    // ????????????dialog
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        recordView = new RecordView(this);
        recordView.callback = this;
        setContentView(recordView);

        surfaceView = recordView.cameraPreview;
        duetSurfaceView = recordView.duetPreview;
        surfaceView.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFill);
        surfaceView.getHolder().addCallback(this);

//        mediaPlayer = new MediaPlayer();

        // ???????????????
        initData();

    }

    /**
     * ??????????????????????????????
     */
    private void initData() {
        recordView.progressView.recordingMedia = recordingMedia;
        recordView.progressView.medias = medias;
        recordView.progressView.longestVideoSeconds = mMaxRecordTime;
        // intent data

        Intent intent = getIntent();
        if (intent != null){
            duetMediaPath = intent.getStringExtra("duet_media_path");
            float duetDuration = intent.getFloatExtra("duration", 0);
            if (duetDuration > 0){
                // ????????????????????????
                mMaxRecordTime = duetDuration > mMaxRecordTime ? mMaxRecordTime : duetDuration;
                recordView.progressView.longestVideoSeconds = mMaxRecordTime;
            }

            initRecordView(intent);
            if (!TextUtils.isEmpty(duetMediaPath)){
                setDuetPreview();
                try {
                    duetMediaPlayer = new MediaPlayer();
                    duetMediaPlayer.setOnPreparedListener(this);
                    duetSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                        @Override
                        public void surfaceCreated(SurfaceHolder holder) {
                            duetMediaPlayer.setDisplay(holder);
                        }

                        @Override
                        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                        }

                        @Override
                        public void surfaceDestroyed(SurfaceHolder holder) {

                        }
                    });
                    Log.e(TAG, "???????????????"+ duetMediaPath);
                    duetMediaPlayer.setDataSource(duetMediaPath);
                    duetMediaPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
    }

    /**
     * ???????????????????????? view
     * @param intent
     */
    private void initRecordView(Intent intent) {
        String resolution = intent.getStringExtra(InputActivity.RESOLUTION);
        if (resolution != null) {
            switch (resolution) {
                case "540p":
                    mWidth = 960;
                    mHeight = 544;
                    break;
                case "720p":
                    mWidth = 1280;
                    mHeight = 720;
                    break;
                case "1080p":
                    mWidth = 1920;
                    mHeight = 1088;
                    break;
            }
            mFrameRate = Integer.decode(intent.getStringExtra(InputActivity.FRAME_RATE));
            mVideoBitrate = Integer.decode(intent.getStringExtra(InputActivity.VIDEO_BITRATE)) * 1000;
            mAudioBitrate = Integer.decode(intent.getStringExtra(InputActivity.AUDIO_BITRATE)) * 1000;
        }
        // ??????????????????
        String ratio = intent.getStringExtra(InputActivity.SCREEN_RATE);
        if ("4:3".equals(ratio)){
            previewRatio = 0.75;
            if ("540p".equals(resolution)){
                mWidth = 720;
            }
            if ("1080p".equals(resolution)){
                mWidth = 1440;
            }
            if ("720p".equals(resolution)){
                mWidth = 960;
            }
        } else if ("1:1".equals(ratio)){
            previewRatio = 1;
            mWidth = mHeight;
        }
        Log.e(TAG, "???????????????width = " + mWidth + ", height = " + mHeight + " , ratio = " + previewRatio);
        if (previewRatio != 0.5652){
            // ??????????????????????????????????????????view
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) recordView.cameraPreview.getLayoutParams();
            params.dimensionRatio = "w," + ratio;
            recordView.cameraPreview.setLayoutParams(params);
        }
    }

    /**
     * ???????????????????????????????????????view ?????????
     */
    private void setDuetPreview() {
        if (!TextUtils.isEmpty(duetMediaPath)){
            Point screen = ScreenTools.getScreen(this);
            int width = screen.x / 2;
            int height = (int) (width / previewRatio);

            Log.e(TAG, " width = " + width + ", height=" + height);
            duetSurfaceView.setVisibility(View.VISIBLE);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) duetSurfaceView.getLayoutParams();
            params.constrainedWidth = false;
            params.constrainedHeight = false;
            params.width = width;
            params.height = height;
            duetSurfaceView.setLayoutParams(params);

            ConstraintLayout.LayoutParams params2 = (ConstraintLayout.LayoutParams) surfaceView.getLayoutParams();
            params2.constrainedWidth = false;
            params2.constrainedHeight = false;
            params2.width = width;
            params2.height = height;
            surfaceView.setLayoutParams(params2);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE_MUSIC && resultCode == RESULT_OK && data != null){
            // ???????????????????????????????????????
            mMusic = (MediaModel)data.getSerializableExtra(MusicActivity.RESULT_DATA);
            Log.d(TAG, "music: name=" + mMusic.title + " path=" + mMusic.path);
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(mMusic.path);
                mediaPlayer.prepare();
//                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean cameraGrantResult = false;
        boolean recordAudioGrantResult = false;

        if (requestCode == CHECK_PERMISSION_REQUEST_CODE) {
            for (int x = 0; x < permissions.length; x++) {
                String permission = permissions[x];
                if (permissions[x].equals(Manifest.permission.CAMERA) && grantResults[x] == PackageManager.PERMISSION_GRANTED) {
                    cameraGrantResult = true;
                } else if (permissions[x].equals(Manifest.permission.RECORD_AUDIO) && grantResults[x] == PackageManager.PERMISSION_GRANTED) {
                    recordAudioGrantResult = true;
                }
            }

            if (cameraGrantResult && recordAudioGrantResult) {
                openHardware();
            } else {
                Toast.makeText(getBaseContext(), "??????????????????", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, CHECK_PERMISSION_REQUEST_CODE);
            return;
        }

        openHardware();

        Log.d(TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();

        closeHardware();

        Log.d(TAG, "onStop");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        cameraPreviewWrap.setEffetHandler(effectHandler);
        return cameraPreviewWrap.onTouchEvent(event, this);
//        return super.onTouchEvent(event);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // ?????????????????????
        if (mp == duetMediaPlayer){
//            this.preparedDuetPlayer = true;
        }
    }

    /**
     * ??????????????????
     */
    private void openHardware() {
        // ??????????????????
//        Log.d(TAG, "??????????????????");
        openFrontCamera();
        checkAndInitFlashHard();

        // ??????????????????
//        openBackCamera();

        // ???????????????
        Log.d(TAG, "???????????????");
        int bufferSize = AudioRecord.getMinBufferSize(audioSampleRate, audioChannel, audioFormat);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, audioSampleRate, audioChannel,
                audioFormat, bufferSize);
        if (audioRecordWrap == null) {
            audioRecordWrap = new MVYAudioRecordWrap(audioRecord, bufferSize);
            audioRecordWrap.setAudioRecordListener(this);
        }
        audioRecordWrap.startRecording();
    }

    /**
     * ??????????????????
     */
    private void openFrontCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
        mCurrentCameraID = FRONT_CAMERA_ID;
        camera = Camera.open(mCurrentCameraID); // TODO ?????????????????????????????????
        if (cameraPreviewWrap == null) {
            cameraPreviewWrap = new MVYCameraPreviewWrap(camera);
            cameraPreviewWrap.setPreviewListener(this);
        } else {
            cameraPreviewWrap.setCamera(camera);
        }
        if (previewRatio != 0) cameraPreviewWrap.setCameraSize(previewRatio);
        cameraPreviewWrap.setRotateMode(MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateRight); // TODO ????????????????????????, ????????????
        cameraPreviewWrap.startPreview();
    }

    /**
     * ??????????????????
     */
    private void openBackCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
        Log.d(TAG, "??????????????????");
        mCurrentCameraID = BACK_CAMERA_ID;
        camera = Camera.open(mCurrentCameraID);
        if (cameraPreviewWrap == null) {
            cameraPreviewWrap = new MVYCameraPreviewWrap(camera);
            cameraPreviewWrap.setPreviewListener(this);
        } else {
            cameraPreviewWrap.setCamera(camera);
        }
        if (previewRatio != 0) cameraPreviewWrap.setCameraSize(previewRatio);
        cameraPreviewWrap.setRotateMode(MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateRightFlipHorizontal);
        cameraPreviewWrap.startPreview();
    }

    /**
     * ????????????????????????????????????????????????????????????????????????????????????
     */
    private void checkAndInitFlashHard() {
        // ??????????????????
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            if (TextUtils.isEmpty(parameters.getFlashMode())) {
                recordView.flashButton.setVisibility(View.GONE);
            } else {
                recordView.flashButton.setVisibility(View.VISIBLE);
            }
            recordView.flashButton.setChecked(false);
        }
    }

    /**
     * ??????????????????
     */
    private void closeHardware() {
        // ????????????
        if (camera != null) {
            Log.d(TAG, "????????????");
            cameraPreviewWrap.stopPreview();
            cameraPreviewWrap = null;
            camera.release();
            camera = null;
        }

        // ???????????????
        if (audioRecord != null) {
            Log.d(TAG, "???????????????");
            audioRecordWrap.stop();
            audioRecordWrap = null;
            audioRecord.release();
            audioRecord = null;
        }
        pauseMediaPlayer();

        closeMediaCodec();
    }

    /**
     * ??????????????????
     */
    private void closeMediaPlayer() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    /**
     * ??????????????????
     */
    private void pauseMediaPlayer() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        }
    }

    /**
     * ??????????????????
     */
    @Override
    public void cameraVideoOutput(int texture, int width, int height, long timeStamp) {
        // ????????????
        if (effectHandler != null) {
            effectHandler.processWithTexture(texture, width, height);
        }

        // ??????
//        if (effectHandler != null) {
//            final Bitmap thumb = effectHandler.getCurrentImage(width, height);
//        }

        // ?????????surfaceView
        surfaceView.render(texture, width, height);

        // ??????????????????
        if (videoCodec != null && videoCodecConfigResult) {
            if (mLastTimeStamp == 0) mLastTimeStamp = timeStamp;
//            mCurrentTimeStamp = timeStamp;

            long intervalTime = (timeStamp - mLastTimeStamp) / 1000000;// ??????????????????????????????????????????
            mRecordedTotalDuration += intervalTime;
            mLastTimeStamp = timeStamp;
            Log.e(TAG, "????????????????????????" + timeStamp + "   total time=" + mRecordedTotalDuration);
            if (mRecordedTotalDuration >= mMaxRecordTime){
                // ?????????????????????????????????????????????????????????????????????
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recordView.recordButton.setSelected(false);
                    }
                });
                recordViewStopRecord();
                return;
            }
            recordView.progressView.setRecordingMedia(mRecordedTotalDuration);
            videoCodec.writeImageTexture(texture, width, height, timeStamp);
        }
    }

    /**
     * ?????????????????????
     */
    @Override
    public void audioRecordOutput(ByteBuffer byteBuffer, long timestamp) {
        audioRecordLock.lock();
        // ??????????????????
        if (wavAudioRecord != null && isWavAudioRecord) {
            wavAudioRecord.writePCMByteBuffer(byteBuffer);
        }
        audioRecordLock.unlock();
    }

    @Override
    public void playbackVideoOutput(VideoDecoder decoder, final VideoFrame frame){

    }

    @Override
    public void playbackStop(VideoDecoder decoder) {

    }

    @Override
    public void playbackFinish(VideoDecoder decoder) {

    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // ?????????????????????
        effectHandler = new MVYCameraEffectHandler(getApplicationContext());

        try {
            // ????????????
            effectHandler.setStyle(BitmapFactory.decodeStream(getApplicationContext().getAssets().open("FilterResources/filter/03??????.JPG")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // ??????????????????
        effectHandler.setIntensityOfBeauty(0.8f);

        // ??????????????????
        effectHandler.setIntensityOfStyle(0.8f);

        // ??????????????????
        effectHandler.setZoom(1.0f);

        // ???????????????
        effectHandler.setIntensityOfSaturation(1.0f);

        // ????????????
        effectHandler.setIntensityOfBrightness(1.0f);

        // ????????????
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getAssets().open("FilterResources/icon/01??????.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bitmap != null) {
            MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerModel = new MVYGPUImageStickerFilter.MVYGPUImageStickerModel(bitmap);
            effectHandler.addSticker(stickerModel);

            // ??????????????????
            Matrix.translateM(stickerModel.transformMatrix, 0, -0.5f, 0.8f, 0.0f);
        }


        // ????????????
        TextView textView = new TextView(getBaseContext());
        textView.setText("??????????????????");
        textView.setBackgroundColor(R.color.grayColor);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setDrawingCacheEnabled(true);
        textView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
        bitmap = Bitmap.createBitmap(textView.getDrawingCache());
        textView.destroyDrawingCache();
        MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerModel = new MVYGPUImageStickerFilter.MVYGPUImageStickerModel(bitmap);
        effectHandler.addSticker(stickerModel);

        // ??????????????????
        Matrix.translateM(stickerModel.transformMatrix, 0, -0.5f, 0.7f, 0.0f);
        Matrix.rotateM(stickerModel.transformMatrix, 0, 10.f, 0.f, 0.f, 1.f);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // ??????????????????
        if (effectHandler != null) {
            effectHandler.destroy();
            effectHandler = null;
        }
    }

    @Override
    public void recordViewStartRecord() {
        recordingMedia = new MediaInfoModel();
        recordingMedia.ratio = previewRatio;
        recordingMedia.videoPath = getExternalCacheDir().getAbsolutePath() +  "/" + UUID.randomUUID().toString().replace("-","") + ".mp4";
//        recordingMedia.iFrameVideoPath = getExternalCacheDir().getAbsolutePath() + "/" + UUID.randomUUID().toString().replace("-","") + ".mp4";
        recordingMedia.audioPath = getExternalCacheDir().getAbsolutePath() + "/" + UUID.randomUUID().toString().replace("-","") + ".wav";
//        recordingMedia.speed = 0;
        recordingMedia.height = mHeight;
        recordingMedia.width = mWidth;

        if (mediaPlayer != null && !mediaPlayer.isPlaying()) mediaPlayer.start();

        startMediaCodec();

//        if (duetVideoDecoder != null) {
//            duetVideoDecoder.startDecodeFromFirstFrame();
//        }
        if (duetMediaPlayer != null){
            duetMediaPlayer.start();
        }
    }

    @Override
    public void recordViewStopRecord() {
        pauseMediaPlayer();
        closeMediaCodec();

        // ?????????????????????
        isWavAudioRecord = false;
        videoCodecConfigResult = false;
        
        if (medias.isEmpty()) {
            recordingMedia.videoSeconds = mRecordedTotalDuration;
        } else {
            recordingMedia.videoSeconds = mRecordedTotalDuration - lastRecordDuration;
        }

        lastRecordDuration = mRecordedTotalDuration;
        medias.add(recordingMedia);

        Log.d(TAG, "???????????????????????????" + recordingMedia.videoSeconds + " - " + "????????????????????????" + mRecordedTotalDuration);
        Log.d(TAG, "recorded media list: ");
        Log.d(TAG, medias.toString());

        mLastTimeStamp = 0;// ???????????????????????????????????????

        // ???????????????
        recordView.progressView.medias = medias;
        refreshRecordProgress();
        recordView.progressView.setRecordingMedia(0);

        // ????????????????????????
//        if (duetVideoDecoder != null) {
//            duetVideoDecoder.stopDecoder();
//            duetVideoDecoder.stopPlayer();
//        }
        if (duetMediaPlayer != null){
            duetMediaPlayer.pause();
        }
        // ???????????????15s???????????????????????????
        if (mRecordedTotalDuration >= mMaxRecordTime) {
            enterEditor();
        }
    }

    @Override
    public void switchCamera() {
        if (mCurrentCameraID == FRONT_CAMERA_ID){
            Log.d(TAG, "switch camera to back");
            openBackCamera();
        } else if (mCurrentCameraID == BACK_CAMERA_ID){
            Log.d(TAG, "switch camera to front");
            openFrontCamera();
        }
        checkAndInitFlashHard();
    }

    @Override
    public void switchCameraFlash() {
        cameraPreviewWrap.switchCameraFlashMode();
    }

    @Override
    public void recordViewAlterSpeedRate(int index) {
        switch (index) {
            case 0:
                mSpeedRate = 0.2f;
                break;
            case 1:
                mSpeedRate = 0.8f;
                break;
            case 2:
                mSpeedRate = 1f;
                break;
            case 3:
                mSpeedRate = 1.5f;
                break;
            case 4:
                mSpeedRate = 2f;
                break;
        }
    }

    @Override
    public void recordViewRenderNewStyle(StyleModel model) {
        try {
            effectHandler.setStyle(BitmapFactory.decodeStream(getApplicationContext().getAssets().open(model.path)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void choiceMusic() {
        Intent intent = new Intent(this, MusicActivity.class);
        startActivityForResult(intent, REQUEST_CODE_CHOOSE_MUSIC);
    }

    @Override
    public void nextToEditor() {
        if (medias.size() <= 0) return;
        enterEditor();
    }

    @Override
    public void setIntensityBeauty(float intensityMax, float progress) {
        float intensity = progress / intensityMax * 10;
        effectHandler.setIntensityOfBeauty(intensity);
    }

    @Override
    public void setIntensityBrightness(float max, float progress) {
        effectHandler.setIntensityOfBrightness(progress / 100f);
    }

    @Override
    public void setIntensitySaturability(float max, float progress) {
        // ????????????????????? ???1 ???1.1 ??????

        effectHandler.setIntensityOfSaturation(progress / 100f);
        Log.e(TAG, "saturation " + (progress / 1000f + 1f));
    }

    @Override
    public void duetShoot() {
        Intent intent = new Intent(this, ImportVideoActivity.class);
        Constant.shootMode = 1;
        startActivity(intent);
        finish();
    }

    // ????????????????????????????????????????????????
    @Override
    public void removeRecordedVideoFromLast() {
        if (medias.size() > 0) {
            MediaInfoModel media = medias.get(medias.size() - 1);
            // ??????????????????????????????
            lastRecordDuration -= media.videoSeconds;
            mRecordedTotalDuration -= media.videoSeconds;

            // ?????????????????????????????????
            medias.remove(medias.size() - 1);
            refreshRecordProgress();

            // ?????????????????????????????????????????????
            if (mediaPlayer != null) mediaPlayer.seekTo((int) mRecordedTotalDuration);
        }
    }

    // ?????????????????????????????? rxJava
    private Disposable lastDispoasble;
    @Override
    public void takePicture() {
        // ??????
        if (picture != null){
            picture.recycle();
            picture = null;
        }

        try {
            int width = 736;
            int height = (int) ((double)width / previewRatio / 16) * 16;
            Log.e(TAG, "tack picture, width(mHeight)=" + width + ", height(mWdith)="  + height + ". preview mWidth=" + mWidth + ", mHeight=" + mHeight);
            picture = effectHandler.getCurrentImage(width , height);
            if (picture != null){
                recordView.previewImageView.setImageBitmap(picture);
                recordView.previewImageView.setVisibility(View.VISIBLE);
            }
            Observable.timer(1, TimeUnit.SECONDS)
                    .take(3)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Long>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            if (lastDispoasble != null) lastDispoasble.dispose();
                            lastDispoasble = d;
                        }

                        @Override
                        public void onNext(Long aLong) {
                        }

                        @Override
                        public void onError(Throwable e) {
                        }

                        @Override
                        public void onComplete() {
                            recordView.previewImageView.setVisibility(View.GONE);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "????????????", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ???????????????
     */
    private void startMediaCodec() {
        // ??????????????????
        int width = mWidth; // ??????????????????????????????90???
        int height = mHeight;
        int bitRate = mVideoBitrate; // ??????: 2Mbps
        int fps = mFrameRate; // ??????: 30
        int iFrameInterval = 1; // GOP: 30

        // ??????????????????
        int audioBitRate = mAudioBitrate; // ??????: 128kbps

        // ???????????????
        CodecInfo codecInfo = getAvcSupportedFormatInfo();
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

        Log.d(TAG, "?????????????????????????????????" + "width = " + width + "height = " + height + "bitRate = " + bitRate
                + "fps = " + fps + "IFrameInterval = " + iFrameInterval + "speedRate = " + mSpeedRate);
        // ????????????
        videoCodec = new MVYMediaCodec(recordingMedia.videoPath, 1);
        videoCodec.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFill);
        videoCodecConfigResult = videoCodec.configureVideoCodecAndStart(width, height, bitRate, fps, iFrameInterval, mSpeedRate);

        audioRecordLock.lock();
        wavAudioRecord = new MVYWavAudioRecord(getBaseContext(), recordingMedia.audioPath, audioSampleRate, 1, mSpeedRate);
        isWavAudioRecord = true;
        audioRecordLock.unlock();
    }

    /**
     * ???????????????
     */
    private void closeMediaCodec() {
        // ????????????
        if (videoCodec != null) {
            Log.d(TAG, "???????????????");
            if (videoCodecConfigResult) {
                videoCodec.finish();
                videoCodec = null;
//                videoCodecConfigResult = false;
            }
        }
        if (wavAudioRecord != null) {
            Log.d(TAG, "??????????????????");
            audioRecordLock.lock();
            if (isWavAudioRecord) {
                wavAudioRecord.finish();
                wavAudioRecord = null;
//                isWavAudioRecord = false;
            }
            audioRecordLock.unlock();
        }
    }

    /**
     * ???????????????
     */
    private void refreshRecordProgress() {
        recordView.progressView.postInvalidate();
    }

    /**
     * ????????????????????????
     */
    private void enterEditor() {
        // ??????????????????????????????????????????????????????
        if (videoCodecConfigResult){
            recordView.recordButton.setSelected(false);
            recordViewStopRecord();
        }

        Intent intent = new Intent(this, EditActivity.class);
        if (mMusic != null){
            intent.putExtra(INTENT_AUDIO_PATH, mMusic);
        }
        // ???????????????????????????????????????
        if (!TextUtils.isEmpty(duetMediaPath)){
            intent = new Intent(this, OutputActivity.class);
            intent.putExtra("duet_path", duetMediaPath);
        }
        intent.putExtra("medias", medias);
        startActivity(intent);
//        finish();
    }
}
