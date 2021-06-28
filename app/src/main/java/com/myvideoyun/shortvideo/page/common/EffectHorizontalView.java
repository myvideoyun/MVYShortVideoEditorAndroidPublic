package com.myvideoyun.shortvideo.page.common;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.GPUImageCustomFilter.MVYGPUImageShortVideoFilter;
import com.myvideoyun.shortvideo.page.effect.model.EffectModel;

import java.io.IOException;

/**
 * Created by 汪洋 on 2019/2/2.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class EffectHorizontalView extends FrameLayout {

    private View contentView;

    private RecyclerView recycleView;

    private StylePlaneAdapter adapter;

    private EffectModel[] modes;

    public StylePlaneOnClickItemListener itemClickListener;
    private boolean mOutskirtsHide = true;
    private int measuredHeight;

    public EffectHorizontalView(Context context) {
        super(context);
        setupView();
    }

    public EffectHorizontalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupView();
    }

    public EffectHorizontalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupView();
    }

    private void setupView() {
        contentView = inflate(getContext(), R.layout.effect_horizontal_view, this);
        recycleView = contentView.findViewById(R.id.style_plane_recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recycleView.setLayoutManager(linearLayoutManager);
        recycleView.setAdapter(adapter = new StylePlaneAdapter());


//        try {
//            setStyles(styleData(getContext()));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        measuredHeight = getMeasuredHeight();
    }

    public void setStyles(EffectModel[] models) {
        this.modes = models;
        adapter.notifyDataSetChanged();
    }

    class StylePlaneAdapter extends RecyclerView.Adapter<StyleViewHolder> {

        @Override
        public StyleViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new StyleViewHolder(new EffectPlaneCell(viewGroup.getContext()));
        }

        @Override
        public void onBindViewHolder(final StyleViewHolder viewHolder, final int i) {
            viewHolder.cell.textView.setText(modes[i].text);
            try {
                viewHolder.cell.imageView.setImageBitmap(BitmapFactory.decodeStream(getResources().getAssets().open(modes[i].thumbnail)));
                viewHolder.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int i = viewHolder.getAdapterPosition();
                        if (itemClickListener != null) itemClickListener.onClickItem(i, modes[i]);
                    }
                });

                viewHolder.itemView.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return false;
                    }
                });
                if (viewHolder.itemView instanceof EffectPlaneCell){
                    EffectPlaneCell cell = (EffectPlaneCell) viewHolder.itemView;
                    cell.setLongTouchListener(new EffectPlaneCell.OnLongTouchListener() {
                        @Override
                        public void onStartTouched(long time) {
                            int i = viewHolder.getAdapterPosition();
                            if (itemClickListener != null) itemClickListener.onStartTouch(i, time, modes[i]);
                        }

                        @Override
                        public void onTouching(long time) {
                            int i = viewHolder.getAdapterPosition();
                            if (itemClickListener != null) itemClickListener.onTouchingItem(i, time, modes[i]);
                        }

                        @Override
                        public void onEndTouched(long time) {
                            int i = viewHolder.getAdapterPosition();
                            if (itemClickListener != null) itemClickListener.onEndTouched(i, time, modes[i]);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return modes != null ? modes.length : 0;
        }
    }

    static class StyleViewHolder extends RecyclerView.ViewHolder {

        EffectPlaneCell cell;

        StyleViewHolder(View itemView) {
            super(itemView);
            if (itemView instanceof EffectPlaneCell) {
                cell = (EffectPlaneCell) itemView;
            }
        }
    }

    public static String[] effects = {
            "基本", "灵魂", "抖动", "黑魔法",
            "虚拟镜像", "萤火", "时光隧道", "躁动",
            "终极变色", "分屏", "幻觉", "70s",
            "酷炫转动", "四分屏", "三分屏", "黑白闪烁",
    };
    public static int [] colors = {
            0xfffe4444, 0xff3f3f3f, 0xffef7a4c, 0xffac021a,
            0xffcb810b, 0xff897104, 0xffebf22c, 0xff9bd60d,
            0xff2b7904, 0xff24f33d, 0xff24f3a3, 0xff11f2ea,
            0xff0790c9, 0xff0c07c9, 0xff600ae1, 0xffd90ae1,
    };
    public static MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE [] types = {
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_NONE,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_SPIRIT_FREED,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_SHAKE,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_BLACK_MAGIC,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_VIRTUAL_MIRROR,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_FLUORESCENCE,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_TIME_TUNNEL,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_DYSPHORIA,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_FINAL_ZELIG,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_SPLIT_SCREEN,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_HALLUCINATION,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_SEVENTYS,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_ROLL_UP,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_FOUR_SCREEN,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_THREE_SCREEN,
            MVYGPUImageShortVideoFilter.MVY_VIDEO_EFFECT_TYPE.MVY_VIDEO_EFFECT_BLACK_WHITE_TWINKLE,
    };



    public void setOnClickItemListener(StylePlaneOnClickItemListener l){
        this.itemClickListener = l;
    }

    public interface StylePlaneOnClickItemListener {
        void onClickItem(int position, EffectModel model);

        void onTouchingItem(int position, long time, EffectModel model);
        void onStartTouch(int position, long time, EffectModel model);
        void onEndTouched(int position, long time, EffectModel model);
    }

}

