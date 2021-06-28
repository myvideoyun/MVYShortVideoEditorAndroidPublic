package com.myvideoyun.shortvideo.tools;

import android.graphics.Bitmap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class FileTools {
    /**
     * 保存bitmap到本地
     * @param filePath
     * @param bitmap
     * @return
     */
    public static String saveBitmapToCache(String filePath, String name, Bitmap bitmap){
        FileOutputStream stream = null;
        String path = null;
        try {
            File file = new File(filePath, name);
            stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            path = file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (stream != null){
                try {
                    stream.close();
                    stream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                    stream = null;
                }
            }
        }
        return path;
    }

    public static Observable<String> copyFileTo(final String sourcePath, final String newPath){
        return Observable.just("").map(new Function<String, String>() {
            @Override
            public String apply(String s) throws Exception {
                FileInputStream fis = null;
                FileOutputStream fos = null;
                try {
                    fis = new FileInputStream(new File(sourcePath));
                    fos = new FileOutputStream(new File(newPath));
                    byte[] buffer = new byte[1024];
                    int len = -1;
                    while ((len = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fis.close();
                    fos.flush();
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        if (fis != null) {
                            fis.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        fis = null;
                        fos = null;
                    }
                }
                finally {
                    if(fis != null)
                        fis.close();
                    if(fos != null)
                        fos.close();
                }
                return newPath;
            }
        }).observeOn(Schedulers.io()).subscribeOn(AndroidSchedulers.mainThread());
    }
}
