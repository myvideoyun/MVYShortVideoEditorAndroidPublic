package com.myvideoyun.shortvideo.page.music;

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

import com.myvideoyun.shortvideo.page.music.model.MediaModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 汪洋 on 2019/2/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class MusicActivity extends AppCompatActivity implements MusicViewCallback, MusicView.MusicOnClickItemListener {

    public static final String RESULT_DATA = "music_result_data";
    MusicView musicView;
    private static final int REQUEST_CODE = 1001;
    private List<MediaModel> musics = new ArrayList<MediaModel>();
    private boolean isNeedToCut = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        musicView = new MusicView(getBaseContext());
        musicView.callback = this;
        setContentView(musicView);
    }

    private void loadMusics() {
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            MediaModel music = null;
            while (cursor.moveToNext()) {
                music = new MediaModel();
                music.title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                music.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                music.duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

                if (music.path.endsWith("mp3") && music.duration > 0L) {
                    musics.add(music);
                }
            }
            cursor.close();
        }
        setViewData();
    }

    private void setViewData() {
        musicView.setMusicOnClickItemListener(this);
        musicView.loadMusicData(musics);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
        } else {
            loadMusics();
        }
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
                loadMusics();
            } else {
                Toast.makeText(this, "访问媒体权限拒绝", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClickedItem(MediaModel music) {
        // 选中音乐条目，回调至上个页面
        Intent data = new Intent();
        data.putExtra(RESULT_DATA, music);
        setResult(RESULT_OK, data);
        finish();
    }
}
