package com.myvideoyun.shortvideo.page.input.image.transition;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;

import com.myvideoyun.shortvideo.GPUImage.MVYGPUImageConstants;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.inputOutput.MVYGPUImageTextureTransitionInput;
import com.myvideoyun.shortvideo.MVYImageEffectHandler;
import com.myvideoyun.shortvideo.MVYPreviewView;
import com.myvideoyun.shortvideo.page.edit.EditActivity;
import com.myvideoyun.shortvideo.page.music.model.MediaModel;
import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;
import com.myvideoyun.shortvideo.recordTool.MVYMediaCodec;
import com.myvideoyun.shortvideo.recordTool.MVYMediaCodecHelper;
import com.myvideoyun.shortvideo.recordTool.MVYWavAudioRecord;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.myvideoyun.shortvideo.recordTool.MVYMediaCodecHelper.getAvcSupportedFormatInfo;

public class ImageTransitionActivity extends AppCompatActivity implements SurfaceHolder.Callback, ImageTransitionView.ImageTransitionViewCallback {

    static final String TAG = "ImageTransitionActivity";

    enum ImageTransitionType {
        LeftToRight, TopToBottom, ZoomIn, ZoomOut, RotateAndZoomIn, Transparent
    }

    // UI
    ImageTransitionView editImageView;

    // 预览
    MVYPreviewView surfaceView;

    // 数据
    List<MediaModel> images;
    boolean isInitData = false;

    // timer
    Timer timer = new Timer();

    // 渲染器
    MVYImageEffectHandler imageRender;

    // 渲染状态控制
    boolean canRender = false;
    Lock renderLock = new ReentrantLock();

    // 当前渲染的位置
    int renderIndex = 0;

    // 录制
    MVYMediaCodec videoCodec;
    boolean videoCodecConfigResult;
    MVYWavAudioRecord audioCodec;

    // 视频文件保存地址
    String videoPath;
    String audioPath;

    // 视频合成中菊花
    AlertDialog alertDialog;

    // 录制的帧数
    int recordFrameCount = 0;

    // 每张纹理显示多少帧
    int frameCountOfPreTexture = 60;

    // 当前转场特效
    ImageTransitionType currentImageTransitionType = ImageTransitionType.LeftToRight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        editImageView = new ImageTransitionView(getBaseContext());
        editImageView.callback = this;
        setContentView(editImageView);

        surfaceView = editImageView.previewView;
        surfaceView.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFit);
        surfaceView.getHolder().addCallback(this);

        images = (ArrayList<MediaModel>) getIntent().getSerializableExtra("images");

        // 初始化dialog
        alertDialog = new AlertDialog.Builder(this).setMessage("正在合成视频...").create();
        alertDialog.setCancelable(false);

        // 渲染
        render();
    }

    private void initData() {
        // 设置要渲染的数据
        for (MediaModel mediaModel : images) {
            Bitmap bitmap = BitmapFactory.decodeFile(mediaModel.imagePath);
            MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel textureModel = new MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel(bitmap);
            if (mediaModel.orientation == 90) {
                textureModel.rotation = MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateRight;
            } else if (mediaModel.orientation == 180) {
                textureModel.rotation = MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotate180;
            } else if (mediaModel.orientation == 270) {
                textureModel.rotation = MVYGPUImageConstants.MVYGPUImageRotationMode.kMVYGPUImageRotateLeft;
            }
            imageRender.addCacheTexture(textureModel);
        }
    }

    // MARK: 图片渲染

    void render() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                renderLock.lock();

                if (canRender) {

                    // 初始化渲染器
                    if (imageRender == null) {
                        imageRender = new MVYImageEffectHandler();
                    }

                    // 初始化纹理数据
                    if (!isInitData) {
                        initData();
                        isInitData = true;
                    }

                    // 设置要渲染的图片
                    if (imageRender.getCacheTextures().size() > 0) {
                        imageRender.setRenderTextures(processImageTransition(imageRender.getCacheTextures(), renderIndex));

                        renderIndex++;
                    }

                    // 渲染, 录制
                    if (imageRender.getRenderTextures().size() > 0) {
                        int width = 720;
                        int height = 1280;
                        int texture = imageRender.process(width, height);
                        surfaceView.render(texture, width, height);
                    }
                }

                renderLock.unlock();
            }
        }, 0, 30);
    }

    // MARK: 录制

    void generateVideo() {

        videoPath = getExternalCacheDir() + "/imageTransition.mp4";
        audioPath = getExternalCacheDir() + "/imageTransition.wav";

        alertDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                renderLock.lock();

                if (canRender) {

                    // 初始化渲染器
                    if (imageRender == null) {
                        imageRender = new MVYImageEffectHandler();
                    }

                    // 初始化纹理数据
                    if (!isInitData) {
                        initData();
                        isInitData = true;
                    }

                    // 启动编码器
                    startMediaCodec();

                    // 设置总帧数, 每张图片录制30帧
                    recordFrameCount = frameCountOfPreTexture * (images != null ? images.size() : 0);

                    int renderIndex = 0;

                    // 设置要渲染的图片
                    while (imageRender.getCacheTextures().size() > 0) {
                        imageRender.setRenderTextures(processImageTransition(imageRender.getCacheTextures(), renderIndex));

                        int width = 720;
                        int height = 1280;
                        int texture = imageRender.process(width, height);
                        long timeStamp = renderIndex * 33;

                        videoCodec.writeImageTexture(texture, width, height, timeStamp * 1000 * 1000);

                        ByteBuffer byteBuffer = ByteBuffer.allocate(33 * 16 * 2);
                        byteBuffer.put(new byte[33 * 16 * 2]);
                        byteBuffer.rewind();
                        audioCodec.writePCMByteBuffer(byteBuffer);

                        renderIndex++;

                        if (renderIndex == recordFrameCount) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    alertDialog.dismiss();
                                }
                            });

                            closeMediaCodec();
                            pushToEditActivity();
                            break;
                        }
                    }
                }

                renderLock.unlock();
            }
        }).start();
    }

    // MARK: 数据处理

    List<MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel> processImageTransition(List<MVYGPUImageTextureTransitionInput.MVYGPUImageTextureModel> cacheTextures, int renderIndex) {
        switch (currentImageTransitionType) {
            case LeftToRight:
                return ImageTransition.leftToRight(cacheTextures, renderIndex, frameCountOfPreTexture);
            case TopToBottom:
                return ImageTransition.topToBottom(cacheTextures, renderIndex, frameCountOfPreTexture);
            case ZoomIn:
                return ImageTransition.zoomIn(cacheTextures, renderIndex, frameCountOfPreTexture);
            case ZoomOut:
                return ImageTransition.zoomOut(cacheTextures, renderIndex, frameCountOfPreTexture);
            case RotateAndZoomIn:
                return ImageTransition.rotateAndZoomIn(cacheTextures, renderIndex, frameCountOfPreTexture);
            case Transparent:
                return ImageTransition.transparent(cacheTextures, renderIndex, frameCountOfPreTexture);
        }

        return null;
    }

    // MARK: SurfaceHolder.Callback

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        renderLock.lock();
        canRender = true;
        renderLock.unlock();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        renderLock.lock();
        canRender = false;

        // 销毁渲染器
        imageRender.destroy();
        imageRender = null;
        renderIndex = 0;

        renderLock.unlock();
    }

    // MARK: 视频录制

    /**
     * 启动编码器
     */
    private void startMediaCodec() {
        // 图像编码参数
        int width = 1280; // 视频编码时图像旋转了90度
        int height = 720;
        int bitRate = 2 * 1024 * 1024; // 码率: 2Mbps
        int fps = 30; // 帧率: 30
        int iFrameInterval = 1; // GOP: 30

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

        Log.d(TAG, "开始编码，初始化参数；" + "width = " + width + "height = " + height + "bitRate = " + bitRate
                + "fps = " + fps + "IFrameInterval = " + iFrameInterval);

        // 启动编码
        videoCodec = new MVYMediaCodec(videoPath, 1);
        videoCodec.setContentMode(MVYGPUImageConstants.MVYGPUImageContentMode.kMVYGPUImageScaleAspectFill);
        videoCodecConfigResult = videoCodec.configureVideoCodecAndStart(width, height, bitRate, fps, iFrameInterval, 1);

        audioCodec = new MVYWavAudioRecord(this, audioPath, 16000, 1, 1);
    }

    /**
     * 关闭编码器
     */
    private void closeMediaCodec() {
        // 关闭编码
        if (videoCodec != null) {
            if (videoCodecConfigResult) {
                Log.d(TAG, "关闭编码器");
                videoCodec.finish();
                videoCodec = null;
                videoCodecConfigResult = false;

            } else {
                Log.d(TAG, "编码失败");
            }
        }

        if (audioCodec != null) {
            audioCodec.finish();
        }
    }

    /**
     * 页面跳转
     */
    void pushToEditActivity() {
//        mediaInfoModels = (ArrayList<MediaInfoModel>) getIntent().getSerializableExtra("medias");
        ArrayList<MediaInfoModel> medias = new ArrayList<>();
        MediaInfoModel media = new MediaInfoModel();
        media.videoPath = videoPath;

        // get videoPath
        File file = new File(videoPath);
        if (!file.exists()) return;
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        retr.setDataSource(videoPath);

        // get height
        String tmp = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        if (tmp == null) return;
        media.height = Long.parseLong(tmp); // 视频高度

        // get width
        tmp = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        if (tmp == null) return;
        media.width = Long.parseLong(tmp); // 视频宽度

        // get duration
        tmp = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (tmp == null) return;
        media.videoSeconds = Long.parseLong(tmp);

        // get audioPath
        file = new File(audioPath);
        if (!file.exists()) return;
        media.audioPath = audioPath;

        medias.add(media);

        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra("medias", medias);
        startActivity(intent);

        finish();
    }

    // MARK: ImageTransitionViewCallback

    @Override
    public void nextBtClick() {
        generateVideo();
    }

    @Override
    public void imageTransitionTypeChange(ImageTransitionActivity.ImageTransitionType type) {
        currentImageTransitionType = type;
    }
}
