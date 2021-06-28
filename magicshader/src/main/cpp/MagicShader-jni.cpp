#include <jni.h>
#include <string.h>
#include "Observer.h"
#include "mvy_magicshader.h"


static JavaVM *jvm = NULL;
static jobject mvyCoreCallback;

void func_mvy_auth_message(int type, int ret, const char *info) {
    if (type == ObserverMsg::MSG_TYPE_AUTH) {
        JNIEnv *env;

        jvm->AttachCurrentThread(&env, NULL);

        if (mvyCoreCallback == NULL) {
            jvm->DetachCurrentThread();
            return;
        }

        jclass clazz = env->GetObjectClass(mvyCoreCallback);
        if (clazz == NULL) {
            jvm->DetachCurrentThread();
            return;
        }

        jmethodID methodID = env->GetMethodID(clazz, "onResult", "(I)V");
        if (methodID == NULL) {
            jvm->DetachCurrentThread();
            return;
        }

        //调用该java方法
        env->CallVoidMethod(mvyCoreCallback, methodID, ret);

        env->DeleteGlobalRef(mvyCoreCallback);

        mvyCoreCallback = NULL;

        jvm->DetachCurrentThread();
    }
}

Observer mvy_auth_observer = {func_mvy_auth_message};

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_shader_MVYLicenseManager_InitLicense(JNIEnv *env, jclass type, jobject context, jstring appKey_, jobject callback) {
    const char *appKey = env->GetStringUTFChars(appKey_, 0);

    jsize length = env->GetStringLength(appKey_);

    env->GetJavaVM(&jvm);

    mvyCoreCallback = env->NewGlobalRef(callback);
    
    MVY_MagicShader_Auth(env, context, appKey, &mvy_auth_observer, length);
    
    env->ReleaseStringUTFChars(appKey_, appKey);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_myvideoyun_shader_MVYMagicShader_Create(JNIEnv *env, jclass type, jint shaderType) {
    return reinterpret_cast<jlong>(MVY_MagicShader_CreateShader(shaderType));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_shader_MVYMagicShader_Destroy(JNIEnv *env, jclass type, jlong render_) {
    if (render_ != 0) {
        MVY_MagicShader_ReleaseShader(reinterpret_cast<void *>(render_));
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_shader_MVYMagicShader_InitGLResource(JNIEnv *env, jclass type, jlong render_) {
    if (render_ != 0) {
        MVY_MagicShader_InitGL((void *)render_);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_shader_MVYMagicShader_DeinitGLResource(JNIEnv *env, jclass type, jlong render_) {
    if (render_ != 0) {
        MVY_MagicShader_DeinitGL((void *) render_);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_shader_MVYMagicShader_Restart(JNIEnv *env, jclass type, jlong render_) {
    if (render_ != 0) {
//        MVY_MagicShader_DeinitGL((void *) render_);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_shader_MVYMagicShader_Draw(JNIEnv *env, jclass type, jlong render_, jint textureId, jint x, jint y, jint width, jint height) {
    if (render_ != 0) {
        MVY_MagicShader_Draw((void *)render_, textureId, width, height);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_shader_MVYMagicShader_Set(JNIEnv *env, jclass type, jlong render_, jstring key, jfloat value) {
    const char *cKey = env->GetStringUTFChars(key, JNI_FALSE);

    if (render_ != 0) {
        MVY_MagicShader_SetParam((void *) render_, cKey, (void *) &value);
    }

    env->ReleaseStringUTFChars(key, cKey);
}



