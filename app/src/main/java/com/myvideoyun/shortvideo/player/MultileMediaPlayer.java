package com.myvideoyun.shortvideo.player;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.SurfaceHolder;

import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by yangshunfa on 2019/3/26.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 *
 * 多视频播放器。根据要播放的视频数量创建多个 MediaPlayer 播放多个视频，维护单个播放器的seek/pause/stop/start等方法
 *
 */
public class MultileMediaPlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private static final String TAG = "MultileMediaPlayer";
    private ArrayList<MediaInfoModel> medias;
    private ArrayList<MediaPlayer> players;
    private MediaPlayer playingPlayer;
    private boolean isPrepared = false;
    private boolean isStart = false;
    private int currPosition;

    public MultileMediaPlayer(ArrayList<MediaInfoModel> medias){
        this.medias = medias;
        this.players = new ArrayList<>(medias.size());
        try {
            for (int i = 0; i< medias.size() ; i++){
                MediaPlayer player = new MediaPlayer();
                player.setDataSource(medias.get(i).videoPath);
                player.prepareAsync();
                player.setOnCompletionListener(this);
                player.setLooping(false);
                player.setOnPreparedListener(this);
                this.players.add(player);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "multiple media player init fail" );
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.e(TAG, "multiple media player is prepared. media player is " + mp );
        // 准备播放
        this.isPrepared = true;
        readyStart();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.e(TAG, "播放结束的 MediaPlayer 是：" + mp);
        // 播放完成回调，播放下一个
        if (currPosition < players.size() - 1){
            currPosition++;
            playingPlayer = players.get(currPosition);
            playingPlayer.start();
        }
    }

    public void start(){
        this.isStart = true;
        Log.e(TAG, "multiple media player need to start" );
        readyStart();
    }

    private void readyStart() {
        Log.e(TAG, "multiple media player is playing...  prepared = " + isPrepared + " , isStart = " + isStart);
        if (isPrepared && isStart) {
            if (playingPlayer == null) {
                currPosition = 0;
                playingPlayer = players.get(currPosition);
            }
            playingPlayer.start();
        }
    }

    public void seekTo(int msec){
        // 找到当前msed 对应第几个视频的位置
        float realMsec = msec;
        float tempSec = 0;
        int index = 0;
        for (; index < medias.size(); index++) {
             tempSec += medias.get(index).videoSeconds;
             if (msec <= tempSec){
                 // 找到当前视频位置
                 realMsec = msec - (tempSec - medias.get(index).videoSeconds);
                 break;
             }
        }
        MediaPlayer newPlayer = players.get(index);
        if (playingPlayer == null){
            this.playingPlayer = newPlayer;
        }
        if (playingPlayer != newPlayer){
            playingPlayer.pause();
            playingPlayer.seekTo(0);
            playingPlayer = newPlayer;
        }
        this.playingPlayer.seekTo((int) realMsec);
    }

    public void pause(){
        Log.e(TAG, "multiple media player pause" );
        this.isStart = false;
        if (playingPlayer != null && playingPlayer.isPlaying())
            playingPlayer.pause();
    }

    public void stop(){
        this.isStart = false;
        if (playingPlayer != null && playingPlayer.isPlaying()){
            playingPlayer.stop();
        }
    }

    public void release(){
        Log.e(TAG, "multiple media player release" );
        this.isStart = false;
        if (playingPlayer != null){
            playingPlayer.release();
        }
    }

    public void destroyAllPlayer(){
        this.isStart = false;
        if (playingPlayer != null){
            playingPlayer.stop();
            playingPlayer = null;
        }
        for (int i = 0; i < medias.size(); i++) {
            MediaPlayer mediaPlayer = players.get(i);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        players.clear();
        medias.clear();
    }

    public void setDisplay(SurfaceHolder holder){
        for (int i = 0; i < players.size(); i++) {
            Log.e(TAG, "multiple media player setDisplay" );
            MediaPlayer mediaPlayer = players.get(i);
            mediaPlayer.setDisplay(holder);
            mediaPlayer.prepareAsync();
        }
    }
}
