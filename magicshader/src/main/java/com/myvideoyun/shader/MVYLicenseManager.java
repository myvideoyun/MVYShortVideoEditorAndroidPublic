package com.myvideoyun.shader;

import android.content.Context;

public class MVYLicenseManager {

    static {
        System.loadLibrary("MagicShaderJni");
    }

    public static native void InitLicense(Context context, String key, OnResultCallback callback);

    public interface OnResultCallback {
        void onResult(int ret); //0表示成功, 其它表示失败
    }
}
