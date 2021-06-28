package com.myvideoyun.shortvideo;

import android.app.Application;

public class MVYApplication extends Application {
    public static MVYApplication instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
