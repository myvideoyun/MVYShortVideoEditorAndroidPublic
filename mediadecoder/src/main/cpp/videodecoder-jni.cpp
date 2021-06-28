#pragma clang diagnostic ignored"-Wdeprecated-declarations"

#include <jni.h>
#include <string>
#include <sys/param.h>
#include <vector>
#include <android/log.h>
#include <sstream>

#ifdef __cplusplus
extern "C" {
#endif

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>


static const char *TAG = "VideoDecoder";

namespace MVY {

    // 视频帧数据
    typedef struct VideoFrame{
        float width;
        float height;
        int lineSize;
        int rotate;
        double pts;
        double duration;
        double length;
        uint8_t *Y_Data;
        uint8_t *U_Data;
        uint8_t *V_Data;
        int isKeyFrame;
    } VideoFrame;

    // 错误枚举
    typedef enum {
        MovieErrorNone,
        MovieErrorOpenFile,
        MovieErrorStreamInfoNotFound,
        MovieErrorStreamNotFound,
        MovieErrorCodecNotFound,
        MovieErrorOpenCodec,
        MovieErrorAllocateFrame,
    } MovieError;


    typedef struct VideoDecoder {

        // 解码器
        AVCodecContext *mvyCodecContext;

        // 视频流ID
        int mvyVideoStream;

        // 视频帧
        AVFrame *mvyVideoFrame;

        // 文件格式
        AVFormatContext *mvyFormatContext;

        // eos 标记
        int eos;

        // 没有更多帧可读
        int no_decoded_frame;

        // 内部错误
        int internal_error;
    } VideoDecoder;
}

using MVY::VideoDecoder;
using MVY::VideoFrame;

// 创建对象
static VideoDecoder* initVideoDecoder() {
    VideoDecoder *videoDecoder = (VideoDecoder *)malloc(sizeof(VideoDecoder));
    memset(videoDecoder, 0, sizeof(VideoDecoder));
    return videoDecoder;
}

// 销毁对象
static void deinitVideoDecoder(VideoDecoder* videoDecoder) {
    __android_log_write(ANDROID_LOG_ERROR, TAG, "free video Decoder1");
    free(videoDecoder);
}

// 打开文件
static MVY::MovieError openFile(VideoDecoder* videoDecoder, const char *path, const char *fileName) {
    AVFormatContext *formatCtx = NULL;

    int result = avformat_open_input(&formatCtx, path, NULL, NULL);
    if (result < 0) {
        if (formatCtx) {
            avformat_free_context(formatCtx);
        }

        std::ostringstream ss;
        ss << "MovieErrorOpenFile ";
        ss << result;
        ss << " ";
        ss << path;

        __android_log_write(ANDROID_LOG_ERROR, TAG, ss.str().data());
        return MVY::MovieErrorOpenFile;
    }

    if (avformat_find_stream_info(formatCtx, NULL) < 0) {
        avformat_close_input(&formatCtx);

        __android_log_write(ANDROID_LOG_ERROR, TAG, "MovieErrorStreamInfoNotFound");
        return MVY::MovieErrorStreamInfoNotFound;
    }

    av_dump_format(formatCtx, 0, fileName, false);

    videoDecoder->mvyFormatContext = formatCtx;
    return MVY::MovieErrorNone;
}

// 打开视频流
static MVY::MovieError openVideoStream(VideoDecoder* videoDecoder) {
    MVY::MovieError errCode = MVY::MovieErrorStreamNotFound;
    videoDecoder->mvyVideoStream = -1;

    for (int i=0; i<videoDecoder->mvyFormatContext->nb_streams; ++i) {
        if (AVMEDIA_TYPE_VIDEO == videoDecoder->mvyFormatContext->streams[i]->codec->codec_type) {
            if (0 == (videoDecoder->mvyFormatContext->streams[i]->disposition & AV_DISPOSITION_ATTACHED_PIC)) {

                AVCodecContext *codecCtx = videoDecoder->mvyFormatContext->streams[i]->codec;

                AVCodec *codec = avcodec_find_decoder(codecCtx->codec_id);
                if (!codec) {

                    __android_log_write(ANDROID_LOG_ERROR, TAG, "MovieErrorCodecNotFound");
                    errCode = MVY::MovieErrorCodecNotFound;
                    continue;
                }

                codecCtx->thread_count = 4;
                codecCtx->thread_type = FF_THREAD_FRAME;

                if (avcodec_open2(codecCtx, codec, NULL) < 0) {
                    __android_log_write(ANDROID_LOG_ERROR, TAG, "MovieErrorOpenCodec");
                    errCode = MVY::MovieErrorOpenCodec;
                    continue;
                }

                std::ostringstream ss;
                ss << "thread count : ";
                ss << codecCtx->thread_count;
                ss << " codecName : ";
                ss << codecCtx->codec->name;
                __android_log_write(ANDROID_LOG_ERROR, TAG, ss.str().data());

                videoDecoder->mvyVideoFrame = av_frame_alloc();

                if (!videoDecoder->mvyVideoFrame) {
                    avcodec_close(codecCtx);

                    __android_log_write(ANDROID_LOG_ERROR, TAG, "MovieErrorAllocateFrame");
                    errCode = MVY::MovieErrorAllocateFrame;
                    continue;
                }

                videoDecoder->mvyVideoStream = i;
                videoDecoder->mvyCodecContext = codecCtx;

                errCode = MVY::MovieErrorNone;
                break;
            }
        }
    }

    return errCode;
}

static void clearStatus(VideoDecoder *videoDecoder){
    __android_log_print(ANDROID_LOG_ERROR, TAG, "Clear decoder status\n");
    videoDecoder->eos = 0;
    videoDecoder->internal_error = 0;
    videoDecoder->no_decoded_frame = 0;
}

// 关闭视频流
static void closeVideoStream(VideoDecoder* videoDecoder) {
    __android_log_write(ANDROID_LOG_ERROR, TAG, "Close Video Stream");
    videoDecoder->mvyVideoStream = -1;

    clearStatus(videoDecoder);

    if (videoDecoder->mvyVideoFrame) {
        av_free(videoDecoder->mvyVideoFrame);
        videoDecoder->mvyVideoFrame = NULL;
    }

    if (videoDecoder->mvyCodecContext) {
        avcodec_close(videoDecoder->mvyCodecContext);
        videoDecoder->mvyCodecContext = NULL;
    }
}


// 关闭文件
static void closeFile(VideoDecoder* videoDecoder) {
    videoDecoder->mvyVideoStream = NULL;

    if (videoDecoder->mvyFormatContext) {
        avformat_close_input(&(videoDecoder->mvyFormatContext));
        videoDecoder->mvyFormatContext = NULL;
    }
}

// 解码一帧视频
static std::vector<VideoFrame> decodeAFrame(VideoDecoder* videoDecoder) {
    std::vector<VideoFrame> result;

    if (videoDecoder->mvyVideoStream == -1) {
        return result;
    }

    AVPacket packet;

    if(videoDecoder->internal_error == 1){
        __android_log_write(ANDROID_LOG_ERROR, TAG, "Unexpected, decoder has error, should stop now\n");
        return result;
    }

    if(videoDecoder->no_decoded_frame){
        __android_log_write(ANDROID_LOG_ERROR, TAG, "No more video decoded frame\n");
        return result;
    }

    // keep reading until we got one frame or encount end of
    while(true) {
        if(videoDecoder->eos == 0){
            auto code = av_read_frame(videoDecoder->mvyFormatContext, &packet);
            if (code < 0) {
                if (code == AVERROR_EOF) {
                    __android_log_write(ANDROID_LOG_ERROR, TAG, "prepare empty packet to notify decoder, set eos flags\n");
                    packet.data = NULL;
                    packet.size = 0;
                    videoDecoder->eos = 1;
                } else {
                    // some other errors
                    __android_log_write(ANDROID_LOG_ERROR, TAG, "Encount some other error");
                    videoDecoder->internal_error = 1;
                    return result;
                }
            } else if(packet.stream_index != videoDecoder->mvyVideoStream){
                __android_log_write(ANDROID_LOG_ERROR, TAG, "Keep reading\n");
                av_free_packet(&packet);
                continue;
            }
        } else{ // eos is 1
            __android_log_write(ANDROID_LOG_ERROR, TAG, "Flush the decoded frame\n");
            packet.data = NULL;
            packet.size = 0;
        }

        // Usually call avcodec_decode_video2 once for one valid packet.
        // flush the decoder only
        int gotFrame = 0;

        avcodec_decode_video2(videoDecoder->mvyCodecContext, videoDecoder->mvyVideoFrame, &gotFrame, &packet);

        double timeBase = av_q2d(videoDecoder->mvyFormatContext->streams[videoDecoder->mvyVideoStream]->time_base);

        double videoDuration = videoDecoder->mvyFormatContext->streams[videoDecoder->mvyVideoStream]->duration * timeBase * 1000;

        if (gotFrame) {

            VideoFrame frame;
            frame.width = videoDecoder->mvyCodecContext->width;
            frame.height = videoDecoder->mvyCodecContext->height;
            frame.lineSize = videoDecoder->mvyVideoFrame->linesize[0];
            frame.rotate = 0;
            AVDictionaryEntry *tag = NULL;
            tag=av_dict_get(videoDecoder->mvyFormatContext->streams[videoDecoder->mvyVideoStream]->metadata, "rotate", tag, AV_DICT_IGNORE_SUFFIX);
            if (tag != NULL) {
                frame.rotate = atoi(tag->value);
            }

            frame.pts = av_frame_get_best_effort_timestamp(videoDecoder->mvyVideoFrame) * timeBase * 1000;
            frame.duration =  av_frame_get_pkt_duration(videoDecoder->mvyVideoFrame) * timeBase * 1000;
            frame.duration += videoDecoder->mvyVideoFrame->repeat_pict * timeBase * 0.5;
            frame.length = videoDuration;

            frame.Y_Data = videoDecoder->mvyVideoFrame->data[0];
            frame.U_Data = videoDecoder->mvyVideoFrame->data[1];
            frame.V_Data = videoDecoder->mvyVideoFrame->data[2];

            frame.isKeyFrame = videoDecoder->mvyVideoFrame->key_frame;

            std::ostringstream ss;
            ss << " frame width : " << frame.width << " height : " << frame.height;
            ss << " lineSize : " << frame.lineSize;
            ss << " rotate : " << frame.rotate;
            ss << " pts : " << frame.pts << " duration : " << frame.duration;
            ss << " length : " << frame.length;
            ss << " isKeyFrame : " << frame.isKeyFrame;
            __android_log_write(ANDROID_LOG_ERROR, TAG, ss.str().data());

            result.push_back(frame);

            break;
        } else {
            __android_log_write(ANDROID_LOG_ERROR, TAG, "did not get video, read next packet\n");
            if (videoDecoder->eos) {
                __android_log_write(ANDROID_LOG_ERROR, TAG, "no frames in decoder\n");
                videoDecoder->no_decoded_frame = 1;

                break;
            }
        }
    }

    if(packet.data != NULL)
        av_free_packet(&packet);

    return result;
}

// 跳转到后一个I帧
static void backwardSeekTo2(VideoDecoder* videoDecoder, int frameIndex) {
    if (videoDecoder->mvyVideoStream != -1) {
        av_seek_frame(videoDecoder->mvyFormatContext, videoDecoder->mvyVideoStream, frameIndex, AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);
        avcodec_flush_buffers(videoDecoder->mvyCodecContext);
        clearStatus(videoDecoder);
    }
}

// 跳转到前一个I帧
static void forwardSeekTo(VideoDecoder* videoDecoder, double millisecond) {
    if (videoDecoder->mvyVideoStream != -1) {
        double timeBase = av_q2d(videoDecoder->mvyFormatContext->streams[videoDecoder->mvyVideoStream]->time_base);
        int64_t ts = (int64_t)(millisecond / 1000.f / timeBase);
        av_seek_frame(videoDecoder->mvyFormatContext, videoDecoder->mvyVideoStream, ts, 0);
        avcodec_flush_buffers(videoDecoder->mvyCodecContext);
        clearStatus(videoDecoder);
    }
}

// 跳转到后一个I帧
static void backwardSeekTo(VideoDecoder* videoDecoder, double millisecond) {
    if (videoDecoder->mvyVideoStream != -1) {
        double timeBase = av_q2d(videoDecoder->mvyFormatContext->streams[videoDecoder->mvyVideoStream]->time_base);
        int64_t ts = (int64_t)(millisecond / 1000.f / timeBase);
        av_seek_frame(videoDecoder->mvyFormatContext, videoDecoder->mvyVideoStream, ts, AVSEEK_FLAG_BACKWARD);
        avcodec_flush_buffers(videoDecoder->mvyCodecContext);
        clearStatus(videoDecoder);
    }
}

static double getVideoLength(VideoDecoder* videoDecoder) {
    if (videoDecoder->mvyVideoStream != -1) {
        double timeBase = av_q2d(videoDecoder->mvyFormatContext->streams[videoDecoder->mvyVideoStream]->time_base);
        double videoDuration = videoDecoder->mvyFormatContext->streams[videoDecoder->mvyVideoStream]->duration * timeBase * 1000;
        return videoDuration;
    } else {
        return 0;
    }
}

JNIEXPORT void JNICALL
Java_com_myvideoyun_decoder_VideoDecoder_registerFFmpeg(JNIEnv *env, jobject instance_) {
    av_register_all();

    // 打印FFmpeg编解码器基本信息
    char *info = (char *)malloc(4000);

    AVCodec *c_temp = av_codec_next(NULL);

    while (c_temp != NULL) {
        memset(info, 0, 4000);

        if (c_temp->decode != NULL) {
            strcat(info, "[Decode]");
        } else {
            strcat(info, "[Encode]");
        }
        switch (c_temp->type) {
            case AVMEDIA_TYPE_VIDEO:
                strcat(info, "[Video]");
                break;
            case AVMEDIA_TYPE_AUDIO:
                strcat(info, "[Audio]");
                break;
            default:
                strcat(info, "[Other]");
                break;
        }

        sprintf(info, "%s %10s\n", info, c_temp->name);

        __android_log_write(ANDROID_LOG_ERROR, TAG, info);

        c_temp = c_temp->next;
    }

    free(info);
}

JNIEXPORT jboolean JNICALL
Java_com_myvideoyun_decoder_VideoDecoder_isEOS(JNIEnv *env, jobject instance_, jint instance) {
    VideoDecoder *videoDecoder = reinterpret_cast<VideoDecoder *>(instance);
    if(videoDecoder->no_decoded_frame || videoDecoder->internal_error)
        return JNI_TRUE;
    else
        return JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_myvideoyun_decoder_VideoDecoder_initVideoDecoder(JNIEnv *env, jobject instance) {
    return reinterpret_cast<jint>(initVideoDecoder());
}

JNIEXPORT void JNICALL
Java_com_myvideoyun_decoder_VideoDecoder_deinitVideoDecoder(JNIEnv *env, jobject instance_, jint instance) {
    if (instance) {
        VideoDecoder *videoDecoder = reinterpret_cast<VideoDecoder *>(instance);
        deinitVideoDecoder(videoDecoder);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_myvideoyun_decoder_VideoDecoder_openFile(JNIEnv *env, jobject instance_, jint instance, jstring path_, jstring fileName_) {
    const char *path = env->GetStringUTFChars(path_, 0);
    const char *fileName = env->GetStringUTFChars(fileName_, 0);

    if (instance) {
        VideoDecoder *videoDecoder = reinterpret_cast<VideoDecoder *>(instance);

        MVY::MovieError errCode = openFile(videoDecoder, path, fileName);

        if (errCode == MVY::MovieErrorNone) {

            errCode = openVideoStream(videoDecoder);

            if (errCode != MVY::MovieErrorNone) {
                closeVideoStream(videoDecoder);
                closeFile(videoDecoder);

                env->ReleaseStringUTFChars(path_, path);
                env->ReleaseStringUTFChars(fileName_, fileName);

                return JNI_FALSE;
            }
        } else {
            closeFile(videoDecoder);

            env->ReleaseStringUTFChars(path_, path);
            env->ReleaseStringUTFChars(fileName_, fileName);

            return JNI_FALSE;
        }
    }

    env->ReleaseStringUTFChars(path_, path);
    env->ReleaseStringUTFChars(fileName_, fileName);

    return JNI_TRUE;
}

JNIEXPORT jobjectArray JNICALL
Java_com_myvideoyun_decoder_VideoDecoder_decodeAFrame(JNIEnv *env, jobject instance_, jint instance) {

    VideoDecoder *videoDecoder = reinterpret_cast<VideoDecoder *>(instance);

    jclass videoFrameClass = env->FindClass("com/myvideoyun/decoder/VideoFrame");

    jmethodID stringMethodInitId = env->GetMethodID(videoFrameClass, "<init>", "()V");

    jfieldID widthField = env->GetFieldID(videoFrameClass, "width", "I");
    jfieldID heightField = env->GetFieldID(videoFrameClass, "height", "I");
    jfieldID lineSizeField = env->GetFieldID(videoFrameClass, "lineSize", "I");
    jfieldID rotateField = env->GetFieldID(videoFrameClass, "rotate", "I");
    jfieldID ptsField = env->GetFieldID(videoFrameClass, "pts", "D");
    jfieldID durationField = env->GetFieldID(videoFrameClass, "duration", "D");
    jfieldID lengthField = env->GetFieldID(videoFrameClass, "length", "D");
    jfieldID yDataField = env->GetFieldID(videoFrameClass, "yData", "[B");
    jfieldID uDataField = env->GetFieldID(videoFrameClass, "uData", "[B");
    jfieldID vDataField = env->GetFieldID(videoFrameClass, "vData", "[B");
    jfieldID isKeyFrameField = env->GetFieldID(videoFrameClass, "isKeyFrame", "I");

    int index = 0;
    std::vector<VideoFrame> frames = decodeAFrame(videoDecoder);

    jobjectArray objectArray = env->NewObjectArray(frames.size(), videoFrameClass, NULL);

    for (auto frame : frames) {

        jobject videoFrame = env->NewObject(videoFrameClass, stringMethodInitId);

        env->SetIntField(videoFrame, widthField, static_cast<jint>(frame.width));
        env->SetIntField(videoFrame, heightField, static_cast<jint>(frame.height));
        env->SetIntField(videoFrame, lineSizeField, frame.lineSize);
        env->SetIntField(videoFrame, rotateField, frame.rotate);
        env->SetDoubleField(videoFrame, ptsField, frame.pts);
        env->SetDoubleField(videoFrame, durationField, frame.duration);
        env->SetDoubleField(videoFrame, lengthField, frame.length);

        {
            jsize buffSize = static_cast<jsize>(frame.lineSize * frame.height);
            jbyteArray byteArray = env->NewByteArray(buffSize);
            jbyte *bytes = env->GetByteArrayElements(byteArray, NULL);
            memcpy(bytes, frame.Y_Data, static_cast<size_t>(buffSize));
            env->SetByteArrayRegion(byteArray, 0, buffSize, bytes);
            env->SetObjectField(videoFrame, yDataField, byteArray);
        }
        {
            jsize buffSize = static_cast<jsize>(frame.lineSize * frame.height / 4);
            jbyteArray byteArray = env->NewByteArray(buffSize);
            jbyte *bytes = env->GetByteArrayElements(byteArray, NULL);
            memcpy(bytes, frame.U_Data, static_cast<size_t>(buffSize));
            env->SetByteArrayRegion(byteArray, 0, buffSize, bytes);
            env->SetObjectField(videoFrame, uDataField, byteArray);
        }
        {
            jsize buffSize = static_cast<jsize>(frame.lineSize * frame.height / 4);
            jbyteArray byteArray = env->NewByteArray(buffSize);
            jbyte *bytes = env->GetByteArrayElements(byteArray, NULL);
            memcpy(bytes, frame.V_Data, static_cast<size_t>(buffSize));
            env->SetByteArrayRegion(byteArray, 0, buffSize, bytes);
            env->SetObjectField(videoFrame, vDataField, byteArray);
        }

        env->SetIntField(videoFrame, isKeyFrameField, frame.isKeyFrame);

        env->SetObjectArrayElement(objectArray, index, videoFrame);

        ++index;
    }

    return objectArray;
}

JNIEXPORT void JNICALL
Java_com_myvideoyun_decoder_VideoDecoder_forwardSeekTo(JNIEnv *env, jobject instance_, jint instance, jdouble millisecond) {
    if (instance) {
        VideoDecoder *videoDecoder = reinterpret_cast<VideoDecoder *>(instance);
        forwardSeekTo(videoDecoder, millisecond);
    }
}

JNIEXPORT void JNICALL
Java_com_myvideoyun_decoder_VideoDecoder_backwardSeekTo2(JNIEnv *env, jobject instance_, jint instance, jint frameIndex) {
    if (instance) {
        VideoDecoder *videoDecoder = reinterpret_cast<VideoDecoder *>(instance);
        backwardSeekTo2(videoDecoder, frameIndex);
    }
}

JNIEXPORT void JNICALL
Java_com_myvideoyun_decoder_VideoDecoder_backwardSeekTo(JNIEnv *env, jobject instance_, jint instance, jdouble millisecond) {
    if (instance) {
        VideoDecoder *videoDecoder = reinterpret_cast<VideoDecoder *>(instance);
        backwardSeekTo(videoDecoder, millisecond);
    }
}

JNIEXPORT jdouble JNICALL
Java_com_myvideoyun_decoder_VideoDecoder_getVideoLength(JNIEnv *env, jobject instance_, jint instance) {
    if (instance) {
        VideoDecoder *videoDecoder = reinterpret_cast<VideoDecoder *>(instance);
        return getVideoLength(videoDecoder);
    } else {
        return 0;
    }
}

JNIEXPORT void JNICALL
Java_com_myvideoyun_decoder_VideoDecoder_closeFile(JNIEnv *env, jobject instance_, jint instance) {
    if (instance) {
        VideoDecoder *videoDecoder = reinterpret_cast<VideoDecoder *>(instance);

        closeVideoStream(videoDecoder);
        closeFile(videoDecoder);
    }
}

#ifdef __cplusplus
}
#endif
