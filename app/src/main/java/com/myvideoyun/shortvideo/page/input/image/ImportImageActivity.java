package com.myvideoyun.shortvideo.page.input.image;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.myvideoyun.shortvideo.page.input.image.transition.ImageTransitionActivity;
import com.myvideoyun.shortvideo.page.music.model.MediaModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImportImageActivity  extends AppCompatActivity implements ImportImageView.ImportImageViewCallBack {

    static final int REQUEST_CODE = 1001;

    // UI
    ImportImageView importImageView;

    // 数据
    List<MediaModel> images = new ArrayList<>();
    List<MediaModel> selectedImages = new ArrayList<>();

    // 状态
    boolean isPageStop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        importImageView = new ImportImageView(getBaseContext());
        importImageView.callBack = this;
        setContentView(importImageView);
    }

    @Override
    protected void onStart() {
        super.onStart();

        isPageStop = false;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
        } else {
            loadImages();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        isPageStop = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean readExternalStorage = false;
        if (requestCode == REQUEST_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                String permission = permissions[i];
                if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission) && grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    readExternalStorage = true;
                }
            }
            if (readExternalStorage){
                loadImages();
            } else {
                Toast.makeText(this, "访问媒体权限拒绝", Toast.LENGTH_LONG).show();
            }
        }
    }

    // 加载手机中所有的图片
    void loadImages() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                ContentResolver resolver = getContentResolver();
                Cursor cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER);

                if (cursor != null && cursor.moveToFirst()) {

                    while (cursor.moveToNext() && !isPageStop) {

                        MediaModel image = new MediaModel();
                        image.imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));

                        File file = new File(image.imagePath);
                        if (!file.exists()) continue;

                        image.orientation = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION));

                        images.add(image);
                    }

                    cursor.close();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        importImageView.fillData(images);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onClickedItem(MediaModel image) {
        if (image.checked) {
            selectedImages.add(image);
        } else {
            selectedImages.remove(image);
        }
    }

    @Override
    public void onNextClick() {
        Intent intent = new Intent(this, ImageTransitionActivity.class);
        intent.putExtra("images", (ArrayList<MediaModel>) selectedImages);
        startActivity(intent);
    }
}
