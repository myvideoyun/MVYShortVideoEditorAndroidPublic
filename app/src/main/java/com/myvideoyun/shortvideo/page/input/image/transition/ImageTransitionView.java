package com.myvideoyun.shortvideo.page.input.image.transition;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.myvideoyun.shortvideo.MVYPreviewView;
import com.myvideoyun.shortvideo.R;

public class ImageTransitionView extends FrameLayout {

    ImageTransitionViewCallback callback;

    // UI
    View containerView;
    MVYPreviewView previewView;
    Button nextBt;
    Button effectBt1;
    Button effectBt2;
    Button effectBt3;
    Button effectBt4;
    Button effectBt5;
    Button effectBt6;

    public ImageTransitionView(Context context) {
        super(context);

        setupView();
        bindView();
    }

    void setupView() {
        containerView = inflate(getContext(), R.layout.activity_edit_image, this);
        previewView = containerView.findViewById(R.id.edit_image_preview);
        nextBt = containerView.findViewById(R.id.edit_image_next_bt);
        effectBt1 = containerView.findViewById(R.id.edit_image_bt1);
        effectBt2 = containerView.findViewById(R.id.edit_image_bt2);
        effectBt3 = containerView.findViewById(R.id.edit_image_bt3);
        effectBt4 = containerView.findViewById(R.id.edit_image_bt4);
        effectBt5 = containerView.findViewById(R.id.edit_image_bt5);
        effectBt6 = containerView.findViewById(R.id.edit_image_bt6);

        effectBt1.setText("左右");
        effectBt1.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        effectBt1.setPadding(0,0,0,0);
        effectBt1.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.btn_edit_image_1), null, null);
        effectBt1.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5.f, getResources().getDisplayMetrics()));
        effectBt1.setBackgroundColor(Color.TRANSPARENT);

        effectBt2.setText("上下");
        effectBt2.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        effectBt2.setPadding(0,0,0,0);
        effectBt2.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.btn_edit_image_2), null, null);
        effectBt2.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5.f, getResources().getDisplayMetrics()));
        effectBt2.setBackgroundColor(Color.TRANSPARENT);

        effectBt3.setText("放大");
        effectBt3.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        effectBt3.setPadding(0,0,0,0);
        effectBt3.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.btn_edit_image_3), null, null);
        effectBt3.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5.f, getResources().getDisplayMetrics()));
        effectBt3.setBackgroundColor(Color.TRANSPARENT);

        effectBt4.setText("缩小");
        effectBt4.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        effectBt4.setPadding(0,0,0,0);
        effectBt4.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.btn_edit_image_4), null, null);
        effectBt4.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5.f, getResources().getDisplayMetrics()));
        effectBt4.setBackgroundColor(Color.TRANSPARENT);

        effectBt5.setText("旋转");
        effectBt5.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        effectBt5.setPadding(0,0,0,0);
        effectBt5.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.btn_edit_image_5), null, null);
        effectBt5.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5.f, getResources().getDisplayMetrics()));
        effectBt5.setBackgroundColor(Color.TRANSPARENT);

        effectBt6.setText("淡入淡出");
        effectBt6.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        effectBt6.setPadding(0,0,0,0);
        effectBt6.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.btn_edit_image_6), null, null);
        effectBt6.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5.f, getResources().getDisplayMetrics()));
        effectBt6.setBackgroundColor(Color.TRANSPARENT);
    }

    void bindView() {
        nextBt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.nextBtClick();
                }
            }
        });
        effectBt1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.imageTransitionTypeChange(ImageTransitionActivity.ImageTransitionType.LeftToRight);
                }
            }
        });
        effectBt2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.imageTransitionTypeChange(ImageTransitionActivity.ImageTransitionType.TopToBottom);
                }
            }
        });
        effectBt3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.imageTransitionTypeChange(ImageTransitionActivity.ImageTransitionType.ZoomOut);
                }
            }
        });
        effectBt4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.imageTransitionTypeChange(ImageTransitionActivity.ImageTransitionType.ZoomIn);
                }
            }
        });
        effectBt5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.imageTransitionTypeChange(ImageTransitionActivity.ImageTransitionType.RotateAndZoomIn);
                }
            }
        });
        effectBt6.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.imageTransitionTypeChange(ImageTransitionActivity.ImageTransitionType.Transparent);
                }
            }
        });
    }

    public interface ImageTransitionViewCallback {

        void nextBtClick();

        void imageTransitionTypeChange(ImageTransitionActivity.ImageTransitionType type);
    }
}


