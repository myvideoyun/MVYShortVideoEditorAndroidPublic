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
 * Created by 汪洋 on 2019/1/31.
 * Copyright © 2019年 myvideoyun. All rights reserved.
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

    // 预览
    MVYPreviewView surfaceView;

    // 数据处理
    MVYVideoEffectHandler effectHandler;

    // 视频解码
    VideoDecoder videoDecoder;

    // 音频播放
    AudioTrack audioTrack;
    AudioTrack musicTrack;

    // 音频解码
    AudioDecoder audioDecoder;
    AudioDecoder musicDecoder;

    // 音视频数据
    private ArrayList<MediaInfoModel> mediaInfoModels;
    private MediaInfoModel musicInfoModel;

//    private MVYGPUImageStickerFilter.MVYGPUImageStickerModel stickerFilterModel;
    private MVYGPUImageStickerFilter.MVYGPUImageStickerModel subtittleModel;
    private String[] originalAudioPaths;
    // 封面图片 默认为视频第一帧
    private Bitmap mCoverBitmap;
    // 封面图片地址
    private String mCoverPath;

    // 音频播放参数
    int sampleRateInHz = 16000; // 不能修改
    int bufferSizeInBytes = 2048;

    // 选择的背景音乐
    MediaModel music;
    // 字幕
    private MVYGPUImageStickerFilter.MVYGPUImageStickerModel subtitleModel;
    private float mTotalDuration;
    private String[] videoPaths;
    // 判断music 是否裁剪完成
    private boolean cutedMusic;
    // 判断是否进入 onStart
    private boolean isStart;
    // 原声音量
    private float originalVolume = -1;
    // 选择的音乐音量
    private float musicVolume = -1;
    // 是否静音
    private boolean isSoundOffOriginal;
    // 选择的贴纸
    private StyleModel stickerModel;
    // 使用的字幕的图片路径
//    private String subtitlePath;
    private float[] originalTransformMatrix;
    private View slidingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // UI
        editView = new EditView(this);
        editView.callback = this;

        // 画面预览
        surfaceView = editView.preview;
        surfaceView.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFit);
        surfaceView.getHolder().addCallback(this);
        editView.stickerMl.setOnSlidingChhildViewListener(this);
        editView.rangeBar.setOnRangeSelectedListener(this);
        setContentView(editView);

        // 上个页面传递过来的数据
        mediaInfoModels = (ArrayList<MediaInfoModel>) getIntent().getSerializableExtra("medias");
        calculateTotalVideoDuration();
        editView.rangeBar.setProgress(mTotalDuration);

        // 音乐数据
        Serializable musicExtra = getIntent().getSerializableExtra(RecordActivity.INTENT_AUDIO_PATH);
        if (musicExtra != null) {
            music = (MediaModel) musicExtra;
            // 如果音乐不为空，先进行裁剪再初始化
            cutMusicIfNotNull();
        } else {
            Log.d(TAG, "需要编辑的数据：" + mediaInfoModels.toString());

            cutedMusic = true;
            // 初始化视频解码器
            initVideoDecoder();
            // 初始化音频
            initAudioDecoder();
            // 初始化 music
//            if (music != null) initMusicDecoder();
        }
        // 设置贴纸和字幕映射view 的大小
        editView.setStickerMappingView(this, mediaInfoModels.get(0));

        // 清空全局储存的特效
        EffectRestore.effects.clear();
        StickerRestore.clear();
    }

    /**
     * 按照视频长度裁剪选择的音乐
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
                Log.e(TAG, "FFmpeg 命令 : " + cmd);

                FFmpegCMD.exec(cmd, new FFmpegCMD.OnExecCallback() {
                    @Override
                    public void onExecuted(int ret) {
                        Log.e(TAG, "裁剪music 结果：" + ret);
                        if (ret == 0) {
                            music.cutPath = outputMusicPath;
                            // 处理好音乐，开始初始化并播放

                            cutedMusic = true;
                            // 初始化解码器
                            initVideoDecoder();
                            // 录制页面的 audio 跟 music 声音是一样的，需要处理掉
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

        // 根据是否有music 打开关闭音量调节
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
        // 可以快速响应返回事件
        stopMedia();
        destroyMedia();
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOISE && resultCode == RESULT_OK && data != null) {
            this.music = (MediaModel) data.getSerializableExtra(MusicActivity.RESULT_DATA);
            // 设置刚选择的音乐的结束时间为视频长度
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
                Log.e(TAG, "新的封面地址：" + coverBitmapPath);
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
     * 开始解码并播放
     */
    private void startDecodeAndPlayer() {
        if (isStart && cutedMusic) {
            // 视频开始解码
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

            // 音频开始解码播放
            if (audioDecoder != null) {
                audioDecoder.startDecodeFromFirstFrame();
                audioDecoder.startPlayer();
            }

            // music 背景音乐
            if (musicDecoder != null) {
                musicDecoder.startDecodeFromFirstFrame();
                musicDecoder.startPlayer();
            }

            // 音频播放器开始播放
            if (audioTrack != null && !isSoundOffOriginal) {
                audioTrack.play();
            }
            if (musicTrack != null) musicTrack.play();
        }
    }

    /**
     * 计算当前可能有多段视频的总长度
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
     * 初始化音频解码器
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
     * 初始化音乐音频解码器
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
     * 初始化视频解码器
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
     * 停止音视频解码以及播放，不销毁
     */
    private void stopMedia() {
        Log.e(TAG, "Stop A/V decoders and players");
        // 销毁解码器
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

        // 音频播放器停止
        if (audioTrack != null) {
            audioTrack.stop();
        }
        if (musicTrack != null) {
            musicTrack.stop();
        }
    }

    /**
     * 销毁音视频解码器及播放器
     */
    private void destroyMedia() {
        // 销毁解码器
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

        // 销毁音频播放器
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
     * 重置所有播放器和解码器，并开始播放
     */
    private void replayAllPlayer() {
        // 停止解码
        if (audioDecoder != null) audioDecoder.stopDecoder();
        if (musicDecoder != null) musicDecoder.stopDecoder();
        if (videoDecoder != null) videoDecoder.stopDecoder();

        // 关闭播放, 清空缓存的数据
        if (audioDecoder != null) audioDecoder.stopPlayer();
        if (musicDecoder != null) musicDecoder.stopPlayer();
        if (videoDecoder != null) videoDecoder.stopPlayer();

        // 停止播放
//        if (audioTrack != null) audioTrack.stop();
//        if (musicTrack != null) musicTrack.stop();

        // 重新开始解码
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

        // 重新开始播放
        if (videoDecoder != null) videoDecoder.startPlayer();
        if (audioDecoder != null) audioDecoder.startPlayer();
        if (musicDecoder != null) musicDecoder.startPlayer();

//        if (audioTrack != null) audioTrack.play();
//        if (musicTrack != null) musicTrack.play();
    }

    //----------surface回调处理----------

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

    //----------视频解码播放处理---------- start --------
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

        // 渲染yuv数据到纹理
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

    @Override
    public void playbackStop(VideoDecoder decoder) {
        Log.d(TAG, "video playback Stop");
    }

    @Override
    public void playbackFinish(VideoDecoder decoder) {
        Log.d(TAG, "video playback finish");
        if (isStart) replayAllPlayer();
    }
    //----------视频解码播放处理----------  end  ---------

    //----------音频解码播放处理---------- start ---------

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

        if (audioFrame.globalPts == 0 && videoDecoder != null) { // 同步时间戳
            //  等待视频线程1000毫秒, 或者视频线程已经开始播放
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

    //----------音频解码播放处理---------- end ---------

    //----------UI事件处理----------  start ------

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
        // 添加字幕
        if (bitmap != null) {
            String subtitlePath = FileTools.saveBitmapToCache(getExternalCacheDir().getAbsolutePath(), UUID.randomUUID().toString().replace("-", "") + ".png", bitmap);
//            subtitleModel = new MVYGPUImageStickerFilter.MVYGPUImageStickerModel(bitmap);
//
//            //  设置字幕位置
//            Matrix.rotateM(subtitleModel.transformMatrix, 0, 0.f, 0.f, 0.f, 1.f);
//            Matrix.translateM(subtitleModel.transformMatrix, 0, 0.f, -0.6f, 0.0f);
//            effectHandler.addSticker(subtitleModel);

            // 通过 bitmap 的长的一边作为view 的边长
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
        // 带着视频开启新的 record 页面进行合拍
        Intent intent = new Intent(this, RecordActivity.class);
        // 将解码视频获得的视频参数带过去，不到下个页面重新解码获取
        intent.putExtra(InputActivity.RESOLUTION, "720p");// resolution
        intent.putExtra(InputActivity.FRAME_RATE, "30");//frameRate
        intent.putExtra(InputActivity.VIDEO_BITRATE, "4096");//videoBitrate
        intent.putExtra(InputActivity.AUDIO_BITRATE, "64");//audioBitrate
        intent.putExtra(InputActivity.SCREEN_RATE, "16:9");//screenRate
        intent.putExtra("duration", mTotalDuration);//screenRate
        intent.putExtra("medias", mediaInfoModels);
        startActivity(intent);
    }

    // 进入合成页面
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

    // 选取音乐
    @Override
    public void chooseMusic() {
        stopMedia();
        Log.d(TAG, "chooseMusic enter MusicActivity to choice music.");
        Intent intent = new Intent(this, MusicActivity.class);
        startActivityForResult(intent, REQUEST_CODE_CHOISE);
    }
    // 裁剪音乐
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

    // 特效
    @Override
    public void clickEffect() {
        stopMedia();
        Intent intent = new Intent(this, EffectActivity.class);
        intent.putExtra("medias", mediaInfoModels);
        startActivityForResult(intent, REQUEST_CODE_ADD_EFFECT);
    }

    @Override
    public void slidingView(View view, String path, int width, int height, int x, int y) {
        float measuredWidth = editView.stickerMl.getMeasuredWidth() / 2f;//以屏幕中点为中心的 x 点
        float measuredHeight = editView.stickerMl.getMeasuredHeight() / 2f;// 以屏幕中点为中心的 y 点，并且方向与屏幕相反
        float realX = (x + width/2 - measuredWidth) / measuredWidth;
        float realY = (y + height/2 - measuredHeight) / -measuredHeight;
        float scale = (view.getScaleX() * width) / width;
        this.slidingView = view;

//        Log.e(TAG, "坐标原点：(" + measuredWidth +"," + measuredHeight  + "), realX = " + realX + ", realY =" + realY + ", scale =" + scale);

        editView.showRangeBar(true);
        // x 和 y 的值在 -1f 到 1f 之间，中点是 (0,0)，每次变换，都是上次变换后的矩阵加上新的矩阵
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

    //----------UI事件处理----------   end  ------

}
