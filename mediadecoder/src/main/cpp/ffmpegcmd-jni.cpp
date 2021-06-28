#pragma clang diagnostic ignored"-Wdeprecated-declarations"

#include <jni.h>
#include <string>
#include <android/log.h>
#include <sstream>

#ifdef __cplusplus
extern "C" {
#endif

#include "ffmpeg_thread.h"
#include "libavutil/log.h"
#include <libavcodec/jni.h>

static const char *TAG = "FFmpegCMD";

static JavaVM *jvm = NULL;
static jobject ffmpegCMDCallback;
static char **cmd = NULL;
static int cmdCount = 0;

/**
 * c语言-线程回调
 */
static void FFmpegCMD_callback(int ret) {

    av_log_set_callback(NULL);

    JNIEnv *env;

    jvm->AttachCurrentThread(&env, NULL);

    if (ffmpegCMDCallback == NULL) {
        jvm->DetachCurrentThread();
        return;
    }

    jclass clazz = env->GetObjectClass(ffmpegCMDCallback);

    if (clazz == NULL) {
        jvm->DetachCurrentThread();
        return;
    }

    jmethodID methodID = env->GetMethodID(clazz, "onExecuted", "(I)V");
    if (methodID == NULL) {
        jvm->DetachCurrentThread();
        return;
    }

    //调用该java方法
    env->CallVoidMethod(ffmpegCMDCallback, methodID, ret);

    if(cmd != NULL){
        for(int i = 0; i < cmdCount; ++i){
            free(cmd[i]);
        }
        free(cmd);
        cmd = NULL;
    }

    env->DeleteGlobalRef(ffmpegCMDCallback);
    ffmpegCMDCallback = NULL;

    jvm->DetachCurrentThread();
}

static void FFmpegCMD_log_callback(void *ptr, int level, const char *fmt, va_list vl){
    char logBufPrefix[512];
    char logBuffer[1024];
    snprintf(logBufPrefix, 512, "%s", fmt);
    vsnprintf(logBuffer, 1024, logBufPrefix, vl);
    __android_log_write(ANDROID_LOG_ERROR, TAG, logBuffer);
}

JNIEXPORT jint JNICALL
Java_com_myvideoyun_decoder_FFmpegCMD_exec(JNIEnv *env, jclass clazz, jint cmdCount, jobjectArray cmdStrings, jobject callback) {

    av_log_set_level(AV_LOG_DEBUG);
    av_log_set_callback(FFmpegCMD_log_callback);

    env->GetJavaVM(&jvm);

    av_jni_set_java_vm(jvm, NULL);

    ffmpegCMDCallback = env->NewGlobalRef(callback);

    int i = 0;

    if (cmdStrings != NULL) {
        cmd = (char **) malloc(sizeof(char *) * cmdCount);

        for (i = 0; i < cmdCount; ++i) {//转换
            jstring cmdStr = (jstring)env->GetObjectArrayElement(cmdStrings, i);
            int cmdStrLen = env->GetStringUTFLength(cmdStr);

            char *arg = static_cast<char *>(malloc(cmdStrLen + 1));
            memset(arg, 0, cmdStrLen + 1);
            memcpy(arg, env->GetStringUTFChars(cmdStr, 0), cmdStrLen);
            arg[cmdStrLen] = '\0';

            cmd[i] = arg;
        }

        //注册FFmpeg命令执行完毕时的回调
        ffmpeg_thread_callback(FFmpegCMD_callback);

        //新建线程 执行FFmpeg 命令
        ffmpeg_thread_run_cmd(cmdCount, cmd);

    } else {

        FFmpegCMD_callback(-1);
    }

    return 0;

}

#ifdef __cplusplus
}
#endif
