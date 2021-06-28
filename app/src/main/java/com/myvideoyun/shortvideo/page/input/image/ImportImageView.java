package com.myvideoyun.shortvideo.page.input.image;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.page.music.model.MediaModel;

import java.util.ArrayList;
import java.util.List;

public class ImportImageView extends FrameLayout {

    // UI
    View containerView;
    RecyclerView recyclerView;
    Button nextBt;
    ImportImageViewAdapter adapter;

    // 数据
    List<MediaModel> medias = new ArrayList<>();

    // 回调事件
    ImportImageViewCallBack callBack = null;

    public ImportImageView(Context context) {
        super(context);

        setupView();
    }

    private void setupView() {
        containerView = inflate(getContext(), R.layout.activity_import_image, this);
        recyclerView = containerView.findViewById(R.id.importImageRv);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));

        adapter = new ImportImageViewAdapter();
        recyclerView.setAdapter(adapter);

        nextBt = containerView.findViewById(R.id.import_image_next_bt);
        nextBt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callBack != null) callBack.onNextClick();
            }
        });
    }


    // 填充数据
    public void fillData(List<MediaModel> medias) {
        this.medias = medias;
        adapter.notifyDataSetChanged();
    }

    private class ImportImageViewAdapter extends RecyclerView.Adapter<ImportImageViewVH> {

        @NonNull
        @Override
        public ImportImageViewVH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ImportImageViewVH(LayoutInflater.from(getContext()).inflate(R.layout.image_item, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final ImportImageViewVH musicHolder, int i) {
            if (i < medias.size()) {
                final MediaModel media = medias.get(i);
                musicHolder.checkBox.setChecked(media.checked);
                Glide.with(getContext()).load(media.imagePath).into(musicHolder.imageView);
                musicHolder.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        media.checked = !media.checked;
                        musicHolder.checkBox.setChecked(media.checked);

                        if (callBack != null) {
                            callBack.onClickedItem(media);
                        }
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return medias.size();
        }
    }

    class ImportImageViewVH extends RecyclerView.ViewHolder {

        final CheckBox checkBox;
        final ImageView imageView;

        public ImportImageViewVH(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.image_item_cb);
            imageView = itemView.findViewById(R.id.image_item_im);
        }
    }


    public interface ImportImageViewCallBack {
        void onClickedItem(MediaModel music);

        void onNextClick();
    }
}

