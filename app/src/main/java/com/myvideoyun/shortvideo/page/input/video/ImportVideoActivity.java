package com.myvideoyun.shortvideo.page.input.video;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.myvideoyun.shortvideo.page.cut.CutActivity;
import com.myvideoyun.shortvideo.page.music.model.MediaModel;
import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 汪洋 on 2019/2/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class ImportVideoActivity extends AppCompatActivity implements ImportViewCallback, ImportVideoView.MusicOnClickItemListener {

    public static final String RESULT_DATA = "music_result_data";
    private static final String TAG = "ImportVideoActivity";
    ImportVideoView importVideoView;
    private static final int REQUEST_CODE = 1001;
    private List<MediaModel> musics = new ArrayList<MediaModel>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        importVideoView = new ImportVideoView(getBaseContext());
        importVideoView.callback = this;
        setContentView(importVideoView);
    }

    private void loadMusics() {
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER);
        Cursor thumCursor;
        if (cursor != null && cursor.moveToFirst()) {
            MediaModel video = null;
            while (cursor.moveToNext()) {
                video = new MediaModel();
                video.title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
                video.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                File file = new File(video.path);
                if (!file.exists()) continue;
                MediaMetadataRetriever retr = new MediaMetadataRetriever();
                retr.setDataSource(video.path);

                // get height
                String tmp = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                if(tmp == null) continue;
                video.height = Long.parseLong(tmp); // 视频高度

                // get width
                tmp = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                if(tmp == null) continue;
                video.width = Long.parseLong(tmp); // 视频宽度

                // get duration
                tmp = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if(tmp == null) continue;
                video.duration = Long.parseLong(tmp);

                // get name
                video.diaplayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));
                video.size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));

                int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                thumCursor = resolver.query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, null, MediaStore.Video.Thumbnails.VIDEO_ID + "=" + id, null, null);
                if (thumCursor.moveToFirst()){
                    String thumPath = thumCursor.getString(thumCursor.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.DATA));
                    if (!TextUtils.isEmpty(thumPath)) video.thumPath = thumPath;
                }
                thumCursor.close();

                if (video.duration > 1 && video.size > 1) {
                    musics.add(video);
                }

            }
            cursor.close();
        }
        Log.d(TAG, musics.toString());
        setViewData();
    }

    private void setViewData() {
        importVideoView.setMusicOnClickItemListener(this);
        importVideoView.loadMusicData(musics);
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
    public void onClickedItem(MediaModel media) {
        // 选中条目，进入视频剪辑页面
        Log.d(TAG, "choose video:");
        Log.d(TAG, media.toString());
//        Intent data = new Intent(this, EditActivity.class);
        Intent data = new Intent(this, CutActivity.class);
        ArrayList<MediaInfoModel> medias = new ArrayList<>(1);
        MediaInfoModel model = new MediaInfoModel();
        model.videoPath = media.path;
        model.height = media.height;
        model.width = media.width;
//        model.iFrameVideoPath = media.path;
        model.videoSeconds = media.duration;
//        model.audioPath = media.path;
        medias.add(model);
        data.putExtra("medias", medias);
        startActivity(data);
        finish();
//        data.putExtra(RESULT_DATA, media);
//        setResult(RESULT_OK, data);
//        finish();
    }
}
