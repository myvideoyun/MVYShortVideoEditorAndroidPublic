package com.myvideoyun.shortvideo.tools;

import android.app.Activity;
import android.graphics.Point;

public class ScreenTools {
    static Point outSize = new Point();
    public static Point getScreen(Activity context){
        context.getWindowManager().getDefaultDisplay().getSize(outSize);
        return outSize;
    }
}
