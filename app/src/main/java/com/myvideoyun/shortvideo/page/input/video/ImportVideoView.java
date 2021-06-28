package com.myvideoyun.shortvideo.page.input.video;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.page.music.model.MediaModel;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Created by 汪洋 on 2019/2/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class ImportVideoView extends FrameLayout {

    View containerView;
    RecyclerView musicRv;

    ImportViewCallback callback;

    public ImportVideoView(Context context) {
        super(context);

        setupView();
    }

    private void setupView() {
        containerView = inflate(getContext(), R.layout.activity_music, this);
        musicRv = findViewById(R.id.musicRv);
        musicRv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
    }

    private List<MediaModel> mMusics = null;

    public void loadMusicData(List<MediaModel> musics) {
        this.mMusics = musics;
        musicRv.setAdapter(new MusicAdapter());
    }
    private SimpleDateFormat format = new SimpleDateFormat("mm:ss", Locale.getDefault());

    private class MusicAdapter extends RecyclerView.Adapter<MusicHolder> {

        @NonNull
        @Override
        public MusicHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new MusicHolder(LayoutInflater.from(getContext()).inflate(R.layout.music_item, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final MusicHolder musicHolder, int i) {
            if (i < mMusics.size()) {
                MediaModel music = mMusics.get(i);
                musicHolder.titleTv.setText(music.title);
                musicHolder.durationTv.setText(format.format(music.duration));
                musicHolder.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null) mListener.onClickedItem(mMusics.get(musicHolder.getAdapterPosition()));
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mMusics.size();
        }
    }

    private class MusicHolder extends RecyclerView.ViewHolder {

        private final TextView durationTv;
        private final TextView titleTv;

        public MusicHolder(@NonNull View itemView) {
            super(itemView);
            durationTv = itemView.findViewById(R.id.durationTv);
            titleTv = itemView.findViewById(R.id.titleTv);
        }
    }

    private MusicOnClickItemListener mListener = null;

    public void setMusicOnClickItemListener(MusicOnClickItemListener l) {
        this.mListener = l;
    }

    public interface MusicOnClickItemListener {
        void onClickedItem(MediaModel music);
    }

}
