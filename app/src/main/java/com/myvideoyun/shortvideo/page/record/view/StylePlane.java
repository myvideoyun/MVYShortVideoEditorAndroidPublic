package com.myvideoyun.shortvideo.page.record.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.page.record.model.StyleModel;
import com.myvideoyun.shortvideo.page.record.model.StyleModel;

import java.io.IOException;

/**
 * Created by 汪洋 on 2019/2/2.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class StylePlane extends FrameLayout {

    private View contentView;
    private Button hideViewTrigger;
    private FrameLayout titleLayout;
    private TextView titleTv;
    private RecyclerView recycleView;

    private StylePlaneAdapter adapter;

    private StyleModel[] modes;

    public StylePlaneOnHideListener hideListener;
    public StylePlaneOnClickItemListener itemClickListener;
    private boolean mOutskirtsHide = true;
    private int measuredHeight;

    public StylePlane(Context context) {
        super(context);
        setupView();
    }

    public StylePlane(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupView();
    }

    public StylePlane(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupView();
    }

    private void setupView() {
        setVisibility(View.GONE);
        contentView = inflate(getContext(), R.layout.style_plane_layout, this);
        hideViewTrigger = contentView.findViewById(R.id.style_plane_hide_view_trigger);
        titleLayout = contentView.findViewById(R.id.style_plane_title_layout);
        titleTv = contentView.findViewById(R.id.style_plane_title_tv);
        recycleView = contentView.findViewById(R.id.style_plane_recycler_view);

        titleLayout.setBackgroundColor(Color.TRANSPARENT);

        titleTv.setText("滤镜");
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        hideViewTrigger.setBackgroundColor(Color.TRANSPARENT);
        hideViewTrigger.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mOutskirtsHide) {
                    hideViewTrigger.setVisibility(INVISIBLE);
                    return;
                }
                hideUseAnim(true);
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recycleView.setLayoutManager(linearLayoutManager);
        recycleView.setAdapter(adapter = new StylePlaneAdapter());


        try {
            setStyles(styleData(getContext()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        measuredHeight = getMeasuredHeight();
    }

    public void hideUseAnim(final Boolean isHidden) {
        if (!isHidden) {
            this.setVisibility(View.VISIBLE);
            if (hideListener != null) {
                hideListener.onStylePlaneHide(false);
            }
        }

        ObjectAnimator animator;

        if (isHidden) {
            animator = ObjectAnimator.ofFloat(this, "translationY", 0, 500);
        } else {
            animator = ObjectAnimator.ofFloat(this,"translationY", 500,0);
        }

        hideViewTrigger.setEnabled(false);

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (isHidden) {
                    StylePlane.this.setVisibility(View.GONE);
                    if (hideListener != null) {
                        hideListener.onStylePlaneHide(true);
                    }
                } else {
                    hideViewTrigger.setEnabled(true);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        animator.setDuration(300);
        animator.start();
    }

    private void setStyles(StyleModel[] models) {
        this.modes = models;
        adapter.notifyDataSetChanged();
    }

    class StylePlaneAdapter extends RecyclerView.Adapter<StyleViewHolder> {

        @Override
        public StyleViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new StyleViewHolder(new StylePlaneCell(viewGroup.getContext()));
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

        StylePlaneCell cell;

        StyleViewHolder(View itemView) {
            super(itemView);
            if (itemView instanceof StylePlaneCell) {
                cell = (StylePlaneCell) itemView;
            }
        }
    }

    private static StyleModel[] styleData(Context context) throws IOException {
        String[] filters = context.getResources().getAssets().list("FilterResources/filter");
        String[] icons = context.getResources().getAssets().list("FilterResources/icon");

        StyleModel[] models = new StyleModel[filters.length];
        for (int i=0; i<filters.length; i++) {
            StyleModel styleModel = new StyleModel();
            styleModel.path = "FilterResources/filter/" + filters[i];
            styleModel.thumbnail = "FilterResources/icon/" + icons[i];
            styleModel.text = filters[i].substring(2, filters[i].length() - 4);
            models[i] = styleModel;
        }

        return models;
    }

    public void setOnClickItemListener(StylePlaneOnClickItemListener l){
        this.itemClickListener = l;
    }

    public interface StylePlaneOnClickItemListener {
        void onClickItem(int position, StyleModel model);
    }

    public void setOutskirtsHide(boolean hide){
        this.mOutskirtsHide = hide;
    }
}

