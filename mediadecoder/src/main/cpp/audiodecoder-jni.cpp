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
#include <libswresample/swresample.h>

static const char *TAG = "AudioDecoder";


namespace MVY{

    // 音频帧数据
    typedef struct {
        double pts;
        double duration;
        double length;
        int sampleRate;
        int channels;
        uint8_t *buffer;
        size_t bufferSize;
    } AudioFrame;

    // 错误枚举
    typedef enum {
        MovieErrorNone,
        MovieErrorOpenFile,
        MovieErrorStreamInfoNotFound,
        MovieErrorStreamNotFound,
        MovieErrorCodecNotFound,
        MovieErrorOpenCodec,
        MovieErrorAllocateFrame,
        MovieErrorReSampler,
    } MovieError;

    typedef struct  AudioDecoder {
        // 解码器
        AVCodecContext *mvyCodecContext;

        // 视频流ID
        int mvyAudioStream;

        // 视频帧
        AVFrame *mvyAudioFrame;

        // 重采样器
        SwrContext *swrContext;

        // 重采样器 buffer
        uint8_t *swrBuffer;

        // 重采样器 buffer大小
        size_t swrBufferSize;

        // 输出采样率
        int outputSampleRate;

        // 通道数
        int outputChannels;

        // 文件格式
        AVFormatContext *mvyFormatContext;

        // eos 标记
        int eos;

        // 没有更多帧可读
        int no_decoded_frame;

        // 内部错误
        int internal_error;
    } AudioDecoder;
}

using MVY::AudioDecoder;
using MVY::AudioFrame;

// 创建对象
static AudioDecoder* initAudioDecoder() {
    AudioDecoder *audioDecoder = (AudioDecoder *)malloc(sizeof(AudioDecoder));
    memset(audioDecoder, 0, sizeof(AudioDecoder));

    audioDecoder->outputSampleRate = 16000;
    audioDecoder->outputChannels = 1;
    audioDecoder->swrBufferSize = 4096;
    audioDecoder->swrBuffer = static_cast<uint8_t *>(realloc(audioDecoder->swrBuffer, audioDecoder->swrBufferSize));

    return audioDecoder;
}

// 销毁对象
static void deinitAudioDecoder(AudioDecoder* audioDecoder) {
    if (audioDecoder->swrBuffer) {
        free(audioDecoder->swrBuffer);
        audioDecoder->swrBuffer = NULL;
        audioDecoder->swrBufferSize = 0;
    }
    free(audioDecoder);
}

// 打开文件
static MVY::MovieError openFile(AudioDecoder* audioDecoder, const char *path, const char *fileName) {
    AVFormatContext *formatCtx = NULL;

    int result = avformat_open_input(&formatCtx, path, NULL, NULL);
    if (result < 0) {
        if (formatCtx) {
            avformat_free_context(formatCtx);
        }

        std::ostringstream ss;
        ss << "MVY::MovieErrorOpenFile ";
        ss << result;
        ss << " ";
        ss << path;

        __android_log_write(ANDROID_LOG_ERROR, TAG, ss.str().data());
        return MVY::MovieErrorOpenFile;
    }

    if (avformat_find_stream_info(formatCtx, NULL) < 0) {
        avformat_close_input(&formatCtx);

        __android_log_write(ANDROID_LOG_ERROR, TAG, "MVY::MovieErrorStreamInfoNotFound");
        return MVY::MovieErrorStreamInfoNotFound;
    }

    av_dump_format(formatCtx, 0, fileName, false);

    audioDecoder->mvyFormatContext = formatCtx;
    return MVY::MovieErrorNone;
}

// 打开音频流
static MVY::MovieError openAudioStream(AudioDecoder* audioDecoder) {
    MVY::MovieError errCode = MVY::MovieErrorStreamNotFound;
    audioDecoder->mvyAudioStream = -1;

    for (int i=0; i<audioDecoder->mvyFormatContext->nb_streams; ++i) {
        if (AVMEDIA_TYPE_AUDIO == audioDecoder->mvyFormatContext->streams[i]->codec->codec_type) {

            AVCodecContext *codecCtx = audioDecoder->mvyFormatContext->streams[i]->codec;
            SwrContext *swrContext = NULL;

            AVCodec *codec = avcodec_find_decoder(codecCtx->codec_id);
            if (!codec) {

                __android_log_write(ANDROID_LOG_ERROR, TAG, "MVY::MovieErrorCodecNotFound");
                errCode = MVY::MovieErrorCodecNotFound;
                continue;
            }

            codecCtx->thread_count = 4;
            codecCtx->thread_type = FF_THREAD_FRAME;

            if (avcodec_open2(codecCtx, codec, NULL) < 0) {
                __android_log_write(ANDROID_LOG_ERROR, TAG, "MVY::MovieErrorOpenCodec");
                errCode = MVY::MovieErrorOpenCodec;
                avcodec_close(codecCtx);
                continue;
            }

            swrContext = swr_alloc_set_opts(
                    NULL,
                    av_get_default_channel_layout(audioDecoder->outputChannels),
                    AV_SAMPLE_FMT_S16,
                    audioDecoder->outputSampleRate,
                    av_get_default_channel_layout(codecCtx->channels),
                    codecCtx->sample_fmt,
                    codecCtx->sample_rate,
                    0,
                    NULL);

            if (!swrContext || swr_init(swrContext)) {
                __android_log_write(ANDROID_LOG_ERROR, TAG, "MVY::MovieErrorReSampler");
                if (swrContext) {
                    swr_free(&swrContext);
                }
                avcodec_close(codecCtx);
                errCode = MVY::MovieErrorReSampler;
                continue;
            }

            audioDecoder->mvyAudioFrame = av_frame_alloc();

            if (!audioDecoder->mvyAudioFrame) {
                __android_log_write(ANDROID_LOG_ERROR, TAG, "MVY::MovieErrorAllocateFrame");
                if (swrContext) {
                    swr_free(&swrContext);
                }
                avcodec_close(codecCtx);
                errCode = MVY::MovieErrorAllocateFrame;
                continue;
            }

            audioDecoder->mvyAudioStream = i;
            audioDecoder->mvyCodecContext = codecCtx;
            audioDecoder->swrContext = swrContext;

            errCode = MVY::MovieErrorNone;
            break;
        }
    }

    return errCode;
}

static void clearStatus(AudioDecoder *videoDecoder){
    __android_log_print(ANDROID_LOG_ERROR, TAG, "Clear decoder status\n");
    videoDecoder->eos = 0;
    videoDecoder->internal_error = 0;
    videoDecoder->no_decoded_frame = 0;
}

// 关闭视频流
static void closeAudioStream(AudioDecoder* audioDecoder) {
    if (audioDecoder->swrContext) {
        swr_free(&audioDecoder->swrContext);
        audioDecoder->swrContext = NULL;
    }

    audioDecoder->mvyAudioStream = -1;

    clearStatus(audioDecoder);

    if (audioDecoder->mvyAudioFrame) {
        av_free(audioDecoder->mvyAudioFrame);
        audioDecoder->mvyAudioFrame = NULL;
    }

    if (audioDecoder->mvyCodecContext) {
        avcodec_close(audioDecoder->mvyCodecContext);
        audioDecoder->mvyCodecContext = NULL;
    }
}

// 关闭文件
static void closeFile(AudioDecoder* audioDecoder) {
    audioDecoder->mvyAudioStream = NULL;

    if (audioDecoder->mvyFormatContext) {
        avformat_close_input(&(audioDecoder->mvyFormatContext));
        audioDecoder->mvyFormatContext = NULL;
    }
}

// 解码一帧视频
static std::vector<AudioFrame> decodeAFrame(AudioDecoder* audioDecoder) {
    std::vector<AudioFrame> result;

    if (audioDecoder->mvyAudioStream == -1) {
        return result;
    }

    AVPacket packet;

    if(audioDecoder->internal_error == 1) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "Unexpected, decoder has error, should stop now\n");
        return result;
    }

    if(audioDecoder->no_decoded_frame){
        __android_log_write(ANDROID_LOG_ERROR, TAG, "No more audio decoded frame\n");
        return result;
    }

    while(true) {
        if(audioDecoder->eos == 0) {
            auto code = av_read_frame(audioDecoder->mvyFormatContext, &packet);
            if (code < 0) {
                if (code == AVERROR_EOF) {
                    __android_log_write(ANDROID_LOG_ERROR, TAG, "prepare empty packet to notify decoder, set eos flags\n");
                    packet.data = NULL;
                    packet.size = 0;
                    audioDecoder->eos = 1;
                } else {
                    // some other errors
                    __android_log_write(ANDROID_LOG_ERROR, TAG, "Encount some other error");
                    audioDecoder->internal_error = 1;
                    return result;
                }
            } else if(packet.stream_index != audioDecoder->mvyAudioStream) {
                __android_log_write(ANDROID_LOG_ERROR, TAG, "Keep reading\n");
                av_free_packet(&packet);
                continue;
            }
        } else {
            __android_log_write(ANDROID_LOG_ERROR, TAG, "Flush the decoded frame\n");
            packet.data = NULL;
            packet.size = 0;
        }


        int gotFrame = 0;

        avcodec_decode_audio4(audioDecoder->mvyCodecContext, audioDecoder->mvyAudioFrame, &gotFrame,
                              &packet);

        double timeBase = av_q2d(audioDecoder->mvyFormatContext->streams[audioDecoder->mvyAudioStream]->time_base);

        double audioDuration = audioDecoder->mvyFormatContext->streams[audioDecoder->mvyAudioStream]->duration * timeBase * 1000;

        if (gotFrame) {

            int numFrames;

            // 转换采样数据
            numFrames = swr_convert(
                    audioDecoder->swrContext,
                    &(audioDecoder->swrBuffer),
                    (int)audioDecoder->swrBufferSize,
                    (const uint8_t **)audioDecoder->mvyAudioFrame->data,
                    audioDecoder->mvyAudioFrame->nb_samples);

            if (numFrames < 0) {
                __android_log_write(ANDROID_LOG_ERROR, TAG, "fail resample audio");
                break;
            }

            const int bufSize = av_samples_get_buffer_size(
                    NULL,
                    audioDecoder->outputChannels,
                    numFrames,
                    AV_SAMPLE_FMT_S16,
                    1);

            if (bufSize < 0) {
                __android_log_write(ANDROID_LOG_ERROR, TAG, "fail resample audio");
                break;
            }

            // 数据返回
            AudioFrame frame;

            frame.pts = av_frame_get_best_effort_timestamp(audioDecoder->mvyAudioFrame) * timeBase * 1000;
            frame.duration =  av_frame_get_pkt_duration(audioDecoder->mvyAudioFrame) * timeBase * 1000;
            frame.length = audioDuration;
            frame.sampleRate = audioDecoder->outputSampleRate;
            frame.channels = audioDecoder->outputChannels;
            frame.buffer = audioDecoder->swrBuffer;
            frame.bufferSize = (size_t) bufSize;

            std::ostringstream ss;
            ss << " pts : " << frame.pts;
            ss << " duration : " << frame.duration;
            ss << " length : " << frame.length;
            ss << " sampleRate : " << frame.sampleRate;
            ss << " channels : " << frame.channels;
            ss << " bufferSize : " << frame.bufferSize;
            __android_log_write(ANDROID_LOG_ERROR, TAG, ss.str().data());

            result.push_back(frame);

            break;
        } else {
            __android_log_write(ANDROID_LOG_ERROR, TAG, "did not get video, read next packet\n");
            if (audioDecoder->eos) {
                __android_log_write(ANDROID_LOG_ERROR, TAG, "no frames in decoder\n");
                audioDecoder->no_decoded_frame = 1;

                break;
            }
        }
    }

    if(packet.data != NULL)
        av_free_packet(&packet);

    return result;
}

// 跳转到前一个I帧
static void forwardSeekTo(AudioDecoder* audioDecoder, double millisecond) {
    if (audioDecoder->mvyAudioStream != -1) {
        double timeBase = av_q2d(audioDecoder->mvyFormatContext->streams[audioDecoder->mvyAudioStream]->time_base);
        int64_t ts = (int64_t)(millisecond / 1000.f / timeBase);
        av_seek_frame(audioDecoder->mvyFormatContext, audioDecoder->mvyAudioStream, ts, 0);
        avcodec_flush_buffers(audioDecoder->mvyCodecContext);
        clearStatus(audioDecoder);
    }
}

// 跳转到后一个I帧
static void backwardSeekTo(AudioDecoder* audioDecoder, double millisecond) {
    if (audioDecoder->mvyAudioStream != -1) {
        double timeBase = av_q2d(audioDecoder->mvyFormatContext->streams[audioDecoder->mvyAudioStream]->time_base);
        int64_t ts = (int64_t)(millisecond / 1000.f / timeBase);
        av_seek_frame(audioDecoder->mvyFormatContext, audioDecoder->mvyAudioStream, ts, AVSEEK_FLAG_BACKWARD);
        avcodec_flush_buffers(audioDecoder->mvyCodecContext);
        clearStatus(audioDecoder);
    }
}

static double getAudioLength(AudioDecoder* audioDecoder) {
    if (audioDecoder->mvyAudioStream != -1) {
        double timeBase = av_q2d(audioDecoder->mvyFormatContext->streams[audioDecoder->mvyAudioStream]->time_base);
        double videoDuration = audioDecoder->mvyFormatContext->streams[audioDecoder->mvyAudioStream]->duration * timeBase * 1000;
        return videoDuration;
    } else {
        return 0;
    }
}

JNIEXPORT void JNICALL
Java_com_myvideoyun_decoder_AudioDecoder_registerFFmpeg(JNIEnv *env, jobject instance_) {
    av_register_all();
}

JNIEXPORT jint JNICALL
Java_com_myvideoyun_decoder_AudioDecoder_initAudioDecoder(JNIEnv *env, jobject instance) {
    return reinterpret_cast<jint>(initAudioDecoder());
}

JNIEXPORT void JNICALL
Java_com_myvideoyun_decoder_AudioDecoder_deinitAudioDecoder(JNIEnv *env, jobject instance_, jint instance) {
    if (instance) {
        AudioDecoder *audioDecoder = reinterpret_cast<AudioDecoder *>(instance);
        deinitAudioDecoder(audioDecoder);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_myvideoyun_decoder_AudioDecoder_openFile(JNIEnv *env, jobject instance_, jint instance, jstring path_, jstring fileName_) {
    const char *path = env->GetStringUTFChars(path_, 0);
    const char *fileName = env->GetStringUTFChars(fileName_, 0);

    if (instance) {
        AudioDecoder *audioDecoder = reinterpret_cast<AudioDecoder *>(instance);

        MVY::MovieError errCode = openFile(audioDecoder, path, fileName);

        if (errCode == MVY::MovieErrorNone) {

            errCode = openAudioStream(audioDecoder);

            if (errCode != MVY::MovieErrorNone) {
                closeAudioStream(audioDecoder);
                closeFile(audioDecoder);

                env->ReleaseStringUTFChars(path_, path);
                env->ReleaseStringUTFChars(fileName_, fileName);

                return JNI_FALSE;
            }
        } else {
            closeFile(audioDecoder);

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
Java_com_myvideoyun_decoder_AudioDecoder_decodeAFrame(JNIEnv *env, jobject instance_, jint instance) {

    AudioDecoder *audioDecoder = reinterpret_cast<AudioDecoder *>(instance);

    jclass audioFrameClass = env->FindClass("com/myvideoyun/decoder/AudioFrame");

    jmethodID stringMethodInitId = env->GetMethodID(audioFrameClass, "<init>", "()V");

    jfieldID ptsField = env->GetFieldID(audioFrameClass, "pts", "D");
    jfieldID durationField = env->GetFieldID(audioFrameClass, "duration", "D");
    jfieldID lengthField = env->GetFieldID(audioFrameClass, "length", "D");
    jfieldID sampleRateField = env->GetFieldID(audioFrameClass, "sampleRate", "I");
    jfieldID channelsField = env->GetFieldID(audioFrameClass, "channels", "I");
    jfieldID bufferField = env->GetFieldID(audioFrameClass, "buffer", "[B");
    jfieldID bufferSizeField = env->GetFieldID(audioFrameClass, "bufferSize", "I");

    int index = 0;
    std::vector<AudioFrame> frames = decodeAFrame(audioDecoder);

    jobjectArray objectArray = env->NewObjectArray(frames.size(), audioFrameClass, NULL);

    for (auto frame : frames) {

        jobject audioFrame = env->NewObject(audioFrameClass, stringMethodInitId);

        env->SetDoubleField(audioFrame, ptsField, frame.pts);
        env->SetDoubleField(audioFrame, durationField, frame.duration);
        env->SetDoubleField(audioFrame, lengthField, frame.length);
        env->SetIntField(audioFrame, sampleRateField, static_cast<jint>(frame.sampleRate));
        env->SetIntField(audioFrame, channelsField, static_cast<jint>(frame.channels));

        {
            jbyteArray byteArray = env->NewByteArray(frame.bufferSize);
            jbyte *bytes = env->GetByteArrayElements(byteArray, NULL);
            memcpy(bytes, frame.buffer, frame.bufferSize);
            env->SetByteArrayRegion(byteArray, 0, frame.bufferSize, bytes);
            env->SetObjectField(audioFrame, bufferField, byteArray);
        }

        env->SetIntField(audioFrame, bufferSizeField, static_cast<jint>(frame.bufferSize));

        env->SetObjectArrayElement(objectArray, index, audioFrame);

        ++index;
    }

    return objectArray;
}

JNIEXPORT void JNICALL
Java_com_myvideoyun_decoder_AudioDecoder_forwardSeekTo(JNIEnv *env, jobject instance_, jint instance, jdouble millisecond) {
    if (instance) {
        AudioDecoder *audioDecoder = reinterpret_cast<AudioDecoder *>(instance);
        forwardSeekTo(audioDecoder, millisecond);
    }
}
    
JNIEXPORT void JNICALL
Java_com_myvideoyun_decoder_AudioDecoder_backwardSeekTo(JNIEnv *env, jobject instance_, jint instance, jdouble millisecond) {
    if (instance) {
        AudioDecoder *audioDecoder = reinterpret_cast<AudioDecoder *>(instance);
        backwardSeekTo(audioDecoder, millisecond);
    }
}

JNIEXPORT jdouble JNICALL
Java_com_myvideoyun_decoder_AudioDecoder_getAudioLength(JNIEnv *env, jobject instance_, jint instance) {
    if (instance) {
        AudioDecoder *videoDecoder = reinterpret_cast<AudioDecoder *>(instance);
        return getAudioLength(videoDecoder);
    } else {
        return 0;
    }
}

JNIEXPORT void JNICALL
Java_com_myvideoyun_decoder_AudioDecoder_closeFile(JNIEnv *env, jobject instance_, jint instance) {
    if (instance) {
        AudioDecoder *audioDecoder = reinterpret_cast<AudioDecoder *>(instance);

        closeAudioStream(audioDecoder);
        closeFile(audioDecoder);
    }
}

#ifdef __cplusplus
}
#endif

