package com.myvideoyun.shortvideo.page.edit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;

import com.myvideoyun.decoder.AudioDecoder;
import com.myvideoyun.decoder.AudioFrame;
import com.myvideoyun.decoder.FFmpegCMD;
import com.myvideoyun.decoder.VideoDecoder;
import com.myvideoyun.decoder.VideoFrame;
import com.myvideoyun.shortvideo.customUI.MotionLayout;
import com.myvideoyun.shortvideo.customUI.RangeBar;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageShortVideoFilter;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageStickerFilter;
import com.myvideoyun.shortvideo.MVYVideoEffectHandler;
import com.myvideoyun.shortvideo.model.StickerModel;
import com.myvideoyun.shortvideo.page.cover.CoverSelectionActivity;
import com.myvideoyun.shortvideo.page.cutmusic.CutMusicActivity;
import com.myvideoyun.shortvideo.page.effect.EffectActivity;
import com.myvideoyun.shortvideo.page.effect.EffectRestore;
import com.myvideoyun.shortvideo.page.input.InputActivity;
import com.myvideoyun.shortvideo.page.music.MusicActivity;
import com.myvideoyun.shortvideo.page.music.model.MediaModel;
import com.myvideoyun.shortvideo.page.output.OutputActivity;
import com.myvideoyun.shortvideo.page.record.RecordActivity;
import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;
import com.myvideoyun.shortvideo.page.record.model.StyleModel;
import com.myvideoyun.shortvideo.MVYPreviewView;
import com.myvideoyun.shortvideo.tools.FileTools;
import com.myvideoyun.shortvideo.tools.StringUtils;
import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Created by ?????? on 2019/1/31.
 * Copyright ?? 2019??? myvideoyun. All rights reserved.
 */
public class EditActivity extends AppCompatActivity
        implements SurfaceHolder.Callback, VideoDecoder.VideoDecoderListener,
        VideoDecoder.VideoPlaybackListener, AudioDecoder.AudioDecoderListener,
        AudioDecoder.AudioPlaybackListener, EditCallback, MotionLayout.OnSlidingChildViewListener, RangeBar.OnRangeSelectedListener {

    private static final String TAG = "EditActivity";
    public static final int REQUEST_CODE_CHOISE = 1000;
    public static final int REQUEST_CODE_CUT_MUSIC = 1001;
    public static final int REQUEST_CODE_CHOOSE_COVER = 1002;
    public static final int REQUEST_CODE_ADD_EFFECT = 1003;
    public static final String INTENT_DATA_ORIGINAL_VOLUME = "original_volume";
    public static final String INTENT_DATA_MUSIC_VOLUME = "music_volume";
    public static final String INTENT_DATA_SUBTTILE = "subtile";
    public static final String INTENT_DATA_STICKER = "sticker";

    EditView editView;

    // ??????
    MVYPreviewView surfaceView;

    // ????????????
    MVYVideoEffectHandler effectHandler;

    // ????????????
    VideoDecoder videoDecoder;

    // ????????????
    AudioTrack audioTrack;
    AudioTrack musicTrack;

    // ????????????
    AudioDecoder audioDecoder;
    AudioDecoder musicDecoder;

    // ???????????????
    private ArrayList<MediaInfoModel> mediaInfoModels;
    private MediaInfoModel musicInfoModel;

//    private MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerFilterModel;
    private MVYGPUImageStickerFilter.MVYGPUImageStickerModel subtittleModel;
    private String[] originalAudioPaths;
    // ???????????? ????????????????????????
    private Bitmap mCoverBitmap;
    // ??????????????????
    private String mCoverPath;

    // ??????????????????
    int sampleRateInHz = 16000; // ????????????
    int bufferSizeInBytes = 2048;

    // ?????????????????????
    MediaModel music;
    // ??????
    private MVYGPUImageStickerFilter.MVYGPUImageStickerModel subtitleModel;
    private float mTotalDuration;
    private String[] videoPaths;
    // ??????music ??????????????????
    private boolean cutedMusic;
    // ?????????????????? onStart
    private boolean isStart;
    // ????????????
    private float originalVolume = -1;
    // ?????????????????????
    private float musicVolume = -1;
    // ????????????
    private boolean isSoundOffOriginal;
    // ???????????????
    private StyleModel stickerModel;
    // ??????????????????????????????
//    private String subtitlePath;
    private float[] originalTransformMatrix;
    private View slidingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // UI
        editView = new EditView(this);
        editView.callback = this;

        // ????????????
        surfaceView = editView.preview;
        surfaceView.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFit);
        surfaceView.getHolder().addCallback(this);
        editView.stickerMl.setOnSlidingChhildViewListener(this);
        editView.rangeBar.setOnRangeSelectedListener(this);
        setContentView(editView);

        // ?????????????????????????????????
        mediaInfoModels = (ArrayList<MediaInfoModel>) getIntent().getSerializableExtra("medias");
        calculateTotalVideoDuration();
        editView.rangeBar.setProgress(mTotalDuration);

        // ????????????
        Serializable musicExtra = getIntent().getSerializableExtra(RecordActivity.INTENT_AUDIO_PATH);
        if (musicExtra != null) {
            music = (MediaModel) musicExtra;
            // ???????????????????????????????????????????????????
            cutMusicIfNotNull();
        } else {
            Log.d(TAG, "????????????????????????" + mediaInfoModels.toString());

            cutedMusic = true;
            // ????????????????????????
            initVideoDecoder();
            // ???????????????
            initAudioDecoder();
            // ????????? music
//            if (music != null) initMusicDecoder();
        }
        // ???????????????????????????view ?????????
        editView.setStickerMappingView(this, mediaInfoModels.get(0));

        // ???????????????????????????
        EffectRestore.effects.clear();
        StickerRestore.clear();
    }

    /**
     * ???????????????????????????????????????
     */
    private void cutMusicIfNotNull() {
        final String newMusicPath = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + music.path.substring(music.path.lastIndexOf("."));
        FileTools.copyFileTo(music.path, newMusicPath).subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(String s) {
                music.path = newMusicPath;
                String needDuration = StringUtils.convertToSecond((int) mTotalDuration);
                final String outputMusicPath = getExternalCacheDir() + "/" + UUID.randomUUID().toString().replace("-", "") + ".wav";
                String cmd = "ffmpeg -i " + String.format("\"%s\"",music.path) + " -ss " + "00:00:00" + " -t " + needDuration + " -acodec pcm_s16le -ac 1 -ar 16000 " + String.format("\"%s\"",outputMusicPath);
                Log.e(TAG, "FFmpeg ?????? : " + cmd);

                FFmpegCMD.exec(cmd, new FFmpegCMD.OnExecCallback() {
                    @Override
                    public void onExecuted(int ret) {
                        Log.e(TAG, "??????music ?????????" + ret);
                        if (ret == 0) {
                            music.cutPath = outputMusicPath;
                            // ??????????????????????????????????????????

                            cutedMusic = true;
                            // ??????????????????
                            initVideoDecoder();
                            // ??????????????? audio ??? music ????????????????????????????????????
//                          initAudioDecoder();
                            initMusicDecoder();

                            startDecodeAndPlayer();
                        }
                    }
                });
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        isStart = true;

        startDecodeAndPlayer();

        // ???????????????music ????????????????????????
        if (music != null){
            editView.musicBar.setEnabled(true);
        } else {
            editView.musicBar.setEnabled(false);
        }
    }

    @Override
    protected void onStop() {
        isStart = false;
        stopMedia();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        destroyMedia();
        super.onDestroy();
    }

    @Override
    public void finish() {
        stopMedia();
        destroyMedia();
        super.finish();
    }

    @Override
    public void onBackPressed() {
        // ??????????????????????????????
        stopMedia();
        destroyMedia();
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOISE && resultCode == RESULT_OK && data != null) {
            this.music = (MediaModel) data.getSerializableExtra(MusicActivity.RESULT_DATA);
            // ??????????????????????????????????????????????????????
//            if (this.music != null) {
//                this.music.cutPath = music.path;
//            } else {
//                this.music = music;
//            }

            // reinit music decoder when it not null.
            if (musicDecoder == null) {
                initMusicDecoder();
            } else {
                musicDecoder.createNativeAudioDecoder(new String[]{music.path});
            }
        }
        if (requestCode == REQUEST_CODE_CUT_MUSIC && resultCode == RESULT_OK) {
            MediaModel music = (MediaModel) data.getSerializableExtra("music");
            this.music.cutPath = music.path;
            musicDecoder.createNativeAudioDecoder(new String[]{music.path});
        }
        if (requestCode == REQUEST_CODE_CHOOSE_COVER && resultCode == RESULT_OK) {
            String coverBitmapPath = data.getStringExtra(CoverSelectionActivity.DATA_COVER_BITMAP_PATH);
            if (!TextUtils.isEmpty(coverBitmapPath)) {
                Log.e(TAG, "?????????????????????" + coverBitmapPath);
                mCoverPath = coverBitmapPath;
//                mCoverBitmap = BitmapFactory.decodeFile(coverBitmapPath);
            }
        }
        Log.e(TAG, "request code " + REQUEST_CODE_ADD_EFFECT);
        if(requestCode == REQUEST_CODE_ADD_EFFECT){
            if(EffectRestore.isReverse && EffectRestore.reverseVideoPaths != ""){
                Log.e(TAG, "Reset media decoder and set reverseVideoPaths as medias");
                if(videoDecoder != null) {
                    videoDecoder.stopDecoder();
                    videoDecoder.destroyNativeVideoDecoder();
                }
                String []paths = new String[1];
                paths[0] = EffectRestore.reverseVideoPaths;
                mediaInfoModels.get(0).videoPath = EffectRestore.reverseVideoPaths;
                // change it to normal mode;
                EffectRestore.isReverse = false;
                EffectRestore.isNormal = true;
                videoDecoder.createNativeVideoDecoder(paths);
            }
        }
    }

    /**
     * ?????????????????????
     */
    private void startDecodeAndPlayer() {
        if (isStart && cutedMusic) {
            // ??????????????????
            if (videoDecoder != null) {
                // TODO: check if multiple flags are enabled;
                if (EffectRestore.isReverse) {
                    videoDecoder.startDecodeFromLastFrame();
                }
                if(EffectRestore.isNormal){
                    videoDecoder.startDecodeFromFirstFrame();
                }
                if(EffectRestore.isFast){
                    videoDecoder.startFastDecoding();
                }
                if(EffectRestore.isSlow){
                    videoDecoder.startSlowDecoding();
                }

                videoDecoder.startPlayer();
            }

            // ????????????????????????
            if (audioDecoder != null) {
                audioDecoder.startDecodeFromFirstFrame();
                audioDecoder.startPlayer();
            }

            // music ????????????
            if (musicDecoder != null) {
                musicDecoder.startDecodeFromFirstFrame();
                musicDecoder.startPlayer();
            }

            // ???????????????????????????
            if (audioTrack != null && !isSoundOffOriginal) {
                audioTrack.play();
            }
            if (musicTrack != null) musicTrack.play();
        }
    }

    /**
     * ?????????????????????????????????????????????
     */
    private void calculateTotalVideoDuration() {
        originalAudioPaths = new String[mediaInfoModels.size()];
        videoPaths = new String[mediaInfoModels.size()];
        MediaInfoModel tempM;
        for (int i = 0; i < mediaInfoModels.size(); i++) {
            tempM = mediaInfoModels.get(i);
            mTotalDuration += tempM.videoSeconds;
            videoPaths[i] = tempM.videoPath;
            originalAudioPaths[i] = tempM.audioPath;
        }
    }

    /**
     * ????????????????????????
     */
    private void initAudioDecoder() {
        if (audioTrack == null) {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes, AudioTrack.MODE_STREAM);
        }
        if (audioDecoder == null) {
            audioDecoder = new AudioDecoder(true);
            audioDecoder.setDecoderListener(this);
            audioDecoder.setPlaybackListener(this);

            audioDecoder.createNativeAudioDecoder(originalAudioPaths);
        }
    }

    /**
     * ??????????????????????????????
     */
    private void initMusicDecoder() {

        if (music != null) {
            if (musicTrack == null) {
                musicTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes, AudioTrack.MODE_STREAM);
            }

            if (musicDecoder == null) {
                musicDecoder = new AudioDecoder(true);
                musicDecoder.setDecoderListener(this);
                musicDecoder.setPlaybackListener(this);

                String[] paths = {music.path};
                if (!TextUtils.isEmpty(music.cutPath)) paths[0] = music.cutPath;
                musicDecoder.createNativeAudioDecoder(paths);
            }
        }
    }

    /**
     * ????????????????????????
     */
    private void initVideoDecoder() {
        if (videoDecoder == null) {
            videoDecoder = new VideoDecoder(true);
            videoDecoder.setDecoderListener(this);
            videoDecoder.setPlaybackListener(this);

            videoDecoder.createNativeVideoDecoder(videoPaths);
        }
    }

    /**
     * ?????????????????????????????????????????????
     */
    private void stopMedia() {
        Log.e(TAG, "Stop A/V decoders and players");
        // ???????????????
        if (videoDecoder != null) {
            videoDecoder.stopDecoder();
            videoDecoder.stopPlayer();
        }

        if (audioDecoder != null) {
            audioDecoder.stopDecoder();
            audioDecoder.stopPlayer();
        }
        if (musicDecoder != null) {
            musicDecoder.stopDecoder();
            musicDecoder.stopPlayer();
        }

        // ?????????????????????
        if (audioTrack != null) {
            audioTrack.stop();
        }
        if (musicTrack != null) {
            musicTrack.stop();
        }
    }

    /**
     * ????????????????????????????????????
     */
    private void destroyMedia() {
        // ???????????????
        if (videoDecoder != null) {
            videoDecoder.destroyNativeVideoDecoder();
            videoDecoder = null;
        }
        if (audioDecoder != null) {
            audioDecoder.destroyNativeAudioDecoder();
            audioDecoder = null;
        }
        if (musicDecoder != null) {
            musicDecoder.destroyNativeAudioDecoder();
            musicDecoder = null;
        }

        // ?????????????????????
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        if (musicTrack != null) {
            musicTrack.release();
            musicTrack = null;
        }
    }

    /**
     * ???????????????????????????????????????????????????
     */
    private void replayAllPlayer() {
        // ????????????
        if (audioDecoder != null) audioDecoder.stopDecoder();
        if (musicDecoder != null) musicDecoder.stopDecoder();
        if (videoDecoder != null) videoDecoder.stopDecoder();

        // ????????????, ?????????????????????
        if (audioDecoder != null) audioDecoder.stopPlayer();
        if (musicDecoder != null) musicDecoder.stopPlayer();
        if (videoDecoder != null) videoDecoder.stopPlayer();

        // ????????????
//        if (audioTrack != null) audioTrack.stop();
//        if (musicTrack != null) musicTrack.stop();

        // ??????????????????
        if (videoDecoder != null) {
            if (EffectRestore.isReverse) {
                videoDecoder.startDecodeFromFirstFrame();
            }
            if(EffectRestore.isNormal){
                videoDecoder.startDecodeFromFirstFrame();
            }
            if(EffectRestore.isFast){
                videoDecoder.startFastDecoding();
            }
            if(EffectRestore.isSlow){
                videoDecoder.startSlowDecoding();
            }
        }
        if (audioDecoder != null) audioDecoder.startDecodeFromFirstFrame();
        if (musicDecoder != null) musicDecoder.startDecodeFromFirstFrame();

        // ??????????????????
        if (videoDecoder != null) videoDecoder.startPlayer();
        if (audioDecoder != null) audioDecoder.startPlayer();
        if (musicDecoder != null) musicDecoder.startPlayer();

//        if (audioTrack != null) audioTrack.play();
//        if (musicTrack != null) musicTrack.play();
    }

    //----------surface????????????----------

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        effectHandler = new MVYVideoEffectHandler(getApplicationContext());
//        effectHandler.setTypeOfShortVideo(MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_FOUR_SCREEN);
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

    //----------????????????????????????---------- start --------
    @Override
    public List<VideoFrame> decoderVideoOutput(VideoDecoder decoder, VideoFrame videoFrame) {
        List<VideoFrame> frames = new ArrayList<>();
        frames.add(videoFrame);
        return frames;
    }

    @Override
    public void decoderStop(VideoDecoder decoder) {
        Log.d(TAG, "video decoder stop");
    }

    @Override
    public void decoderFinish(VideoDecoder decoder) {
        Log.d(TAG, "video decoder finish");
    }

    @Override
    public void playbackVideoOutput(VideoDecoder decoder, final VideoFrame frame) {

        // ??????yuv???????????????
        if (effectHandler != null) {
            StickerRestore.addAllStickerToVideo(effectHandler, frame);
            EffectRestore.setEffectHandlerType(effectHandler, frame, null);
            int texture;
            if (frame.rotate == 90) {
                texture = effectHandler.processWithYUVData(frame.yData, frame.uData, frame.vData, frame.width, frame.height, frame.lineSize, MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateRight);
            } else if (frame.rotate == 270) {
                texture = effectHandler.processWithYUVData(frame.yData, frame.uData, frame.vData, frame.width, frame.height, frame.lineSize, MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateLeft);
            } else {
                texture = effectHandler.processWithYUVData(frame.yData, frame.uData, frame.vData, frame.width, frame.height, frame.lineSize, MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageNoRotation);
            }

            if (mCoverBitmap == null) {
                if (frame.rotate == 90 || frame.rotate == 270) {
                    mCoverBitmap = effectHandler.getCurrentImage(frame.height, frame.width);
                } else {
                    mCoverBitmap = effectHandler.getCurrentImage(frame.width, frame.height);
                }
            }

            // ?????????surfaceView
            if (surfaceView != null) {
                if (frame.rotate == 90 || frame.rotate == 270) {
                    surfaceView.render(texture, frame.height, frame.width);
                } else {
                    surfaceView.render(texture, frame.width, frame.height);
                }
            }
        }
    }

    @Override
    public void playbackStop(VideoDecoder decoder) {
        Log.d(TAG, "video playback Stop");
    }

    @Override
    public void playbackFinish(VideoDecoder decoder) {
        Log.d(TAG, "video playback finish");
        if (isStart) replayAllPlayer();
    }
    //----------????????????????????????----------  end  ---------

    //----------????????????????????????---------- start ---------

    @Override
    public List<AudioFrame> decoderAudioOutput(AudioDecoder decoder, AudioFrame audioFrame) {
        List<AudioFrame> frames = new ArrayList<>();
        frames.add(audioFrame);
        return frames;
    }

    @Override
    public void decoderStop(AudioDecoder decoder) {
        Log.d(TAG, "audio decoder stop");
    }

    @Override
    public void decoderFinish(AudioDecoder decoder) {
        Log.d(TAG, "audio decoder finish");
    }

    @Override
    public void playbackAudioOutput(AudioDecoder decoder, AudioFrame audioFrame) {
        Log.d(TAG, "playbackAudioOutput frame pts=" + audioFrame.pts);

        if (audioFrame.globalPts == 0 && videoDecoder != null) { // ???????????????
            //  ??????????????????1000??????, ????????????????????????????????????
            for (int x = 0; x < 1000; x++) {
                SystemClock.sleep(1);
                if (videoDecoder.getPlaybackFirstFrameTime() > 0) {
                    break;
                }
            }

            videoDecoder.setPlaybackFirstFrameTime(SystemClock.elapsedRealtime());
        }

        if (decoder == audioDecoder) {
            if (audioTrack != null) {
                audioTrack.write(audioFrame.buffer, 0, audioFrame.bufferSize);
            }
        } else if (decoder == musicDecoder) {
            if (musicTrack != null) {
                musicTrack.write(audioFrame.buffer, 0, audioFrame.bufferSize);
            }
        }
    }

    @Override
    public void playbackStop(AudioDecoder decoder) {

    }

    @Override
    public void playbackFinish(AudioDecoder decoder) {

    }

    //----------????????????????????????---------- end ---------

    //----------UI????????????----------  start ------

    @Override
    public void setOriginalVolume(float max, float progress) {
        if (audioTrack != null) {
            originalVolume = progress / max;
            audioTrack.setStereoVolume(originalVolume, originalVolume);
        }
    }

    @Override
    public void setMusicVolume(float max, float progress) {
        if (musicTrack != null) {
            musicVolume = progress / max;
            musicTrack.setStereoVolume(musicVolume, musicVolume);
        }
    }

    @Override
    public void addSticker(StyleModel model) {
        StickerModel stickerModel = StickerRestore.add(model.thumbnail, editView.addViewToMotionLayout(model.thumbnail));
        StickerRestore.setTime(stickerModel, 0, mTotalDuration);
    }

    @Override
    public void addSubtitle(Bitmap bitmap) {
        // ????????????
        if (bitmap != null) {
            String subtitlePath = FileTools.saveBitmapToCache(getExternalCacheDir().getAbsolutePath(), UUID.randomUUID().toString().replace("-", "") + ".png", bitmap);
//            subtitleModel = new MVYGPUImageStickerFilter.MVYGPUImageStickerModel(bitmap);
//
//            //  ??????????????????
//            Matrix.rotateM(subtitleModel.transformMatrix, 0, 0.f, 0.f, 0.f, 1.f);
//            Matrix.translateM(subtitleModel.transformMatrix, 0, 0.f, -0.6f, 0.0f);
//            effectHandler.addSticker(subtitleModel);

            // ?????? bitmap ?????????????????????view ?????????
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(subtitlePath, opts);

            StickerModel stickerModel = StickerRestore.addSubtitle(subtitlePath,
                    editView.addViewToMotionLayout(subtitlePath, (int) (Math.max(opts.outHeight, opts.outWidth) * 1.2f)));
            StickerRestore.setTime(stickerModel, 0, mTotalDuration);
        }
    }

    @Override
    public void switchAudioToOriginal(boolean isChecked) {
        isSoundOffOriginal = !isChecked;
        if (isChecked) {
            audioTrack.play();
        } else {
            audioTrack.stop();
        }
    }

    @Override
    public void choiseCover() {
        stopMedia();
        Intent intent = new Intent(this, CoverSelectionActivity.class);
        intent.putExtra("medias", mediaInfoModels);
        startActivityForResult(intent, REQUEST_CODE_CHOOSE_COVER);
    }

    @Override
    public void duetShoot() {
        stopMedia();
        // ???????????????????????? record ??????????????????
        Intent intent = new Intent(this, RecordActivity.class);
        // ????????????????????????????????????????????????????????????????????????????????????
        intent.putExtra(InputActivity.RESOLUTION, "720p");// resolution
        intent.putExtra(InputActivity.FRAME_RATE, "30");//frameRate
        intent.putExtra(InputActivity.VIDEO_BITRATE, "4096");//videoBitrate
        intent.putExtra(InputActivity.AUDIO_BITRATE, "64");//audioBitrate
        intent.putExtra(InputActivity.SCREEN_RATE, "16:9");//screenRate
        intent.putExtra("duration", mTotalDuration);//screenRate
        intent.putExtra("medias", mediaInfoModels);
        startActivity(intent);
    }

    // ??????????????????
    @Override
    public void goNextOutput() {
        stopMedia();
        Intent intent = new Intent(this, OutputActivity.class);
        intent.putExtra("medias", mediaInfoModels);
        intent.putExtra("music", music);
        if (isSoundOffOriginal) originalVolume = 0;
        intent.putExtra(INTENT_DATA_ORIGINAL_VOLUME, originalVolume);
        intent.putExtra(INTENT_DATA_MUSIC_VOLUME, musicVolume);
        if (stickerModel != null) intent.putExtra(INTENT_DATA_STICKER, stickerModel.thumbnail);
//        if (!TextUtils.isEmpty(subtitlePath)) intent.putExtra(INTENT_DATA_SUBTTILE, subtitlePath);
        startActivity(intent);
    }

    // ????????????
    @Override
    public void chooseMusic() {
        stopMedia();
        Log.d(TAG, "chooseMusic enter MusicActivity to choice music.");
        Intent intent = new Intent(this, MusicActivity.class);
        startActivityForResult(intent, REQUEST_CODE_CHOISE);
    }
    // ????????????
    @Override
    public void cutMusic() {
        if (music != null) {
            stopMedia();
            Intent intent = new Intent(this, CutMusicActivity.class);
            intent.putExtra("medias", mediaInfoModels);
            intent.putExtra("music", music);
            startActivityForResult(intent, REQUEST_CODE_CUT_MUSIC);
        }
    }

    // ??????
    @Override
    public void clickEffect() {
        stopMedia();
        Intent intent = new Intent(this, EffectActivity.class);
        intent.putExtra("medias", mediaInfoModels);
        startActivityForResult(intent, REQUEST_CODE_ADD_EFFECT);
    }

    @Override
    public void slidingView(View view, String path, int width, int height, int x, int y) {
        float measuredWidth = editView.stickerMl.getMeasuredWidth() / 2f;//??????????????????????????? x ???
        float measuredHeight = editView.stickerMl.getMeasuredHeight() / 2f;// ??????????????????????????? y ?????????????????????????????????
        float realX = (x + width/2 - measuredWidth) / measuredWidth;
        float realY = (y + height/2 - measuredHeight) / -measuredHeight;
        float scale = (view.getScaleX() * width) / width;
        this.slidingView = view;

//        Log.e(TAG, "???????????????(" + measuredWidth +"," + measuredHeight  + "), realX = " + realX + ", realY =" + realY + ", scale =" + scale);

        editView.showRangeBar(true);
        // x ??? y ????????? -1f ??? 1f ?????????????????? (0,0)??????????????????????????????????????????????????????????????????
        StickerRestore.setStickerTranslateScale(view, realX, realY, scale);
    }

    @Override
    public void start(View view, String path, int width, int height, int x, int y) {

    }

    @Override
    public void end(View view, String path, int width, int height, int x, int y) {
//        editView.showRangeBar(false);
    }

    @Override
    public void onRangeSelected(int left, int right) {
        if (slidingView != null){
            StickerRestore.setTime(slidingView, left, right);
        }
    }

    //----------UI????????????----------   end  ------

}
