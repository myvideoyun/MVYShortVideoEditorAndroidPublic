package com.myvideoyun.shortvideo.customUI;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * Created by 汪洋 on 2019/2/1.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class RadioGroup extends LinearLayout {

    private String[] texts;
    private String selectedText;

    public RadioGroup(Context context) {
        super(context);
        setupView(null);
    }

    public RadioGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupView(null);
    }

    public RadioGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupView(null);
    }

    public void setupView(String[] texts) {
        if (texts == null) {
            return;
        }

        this.texts = texts;

        removeAllViews();

        for (String text : texts) {
            LayoutParams params = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
            params.weight = 1;
            final Button button = new Button(getContext());
            button.setLayoutParams(params);

            button.setText(text);
            button.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_selected},{}},
                    new int[]{getResources().getColor(android.R.color.holo_blue_dark), getResources().getColor(android.R.color.darker_gray)}));
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            button.setBackgroundColor(Color.TRANSPARENT);

            this.addView(button);

            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelectedText(((Button)v).getText().toString());
                }
            });
        }
    }

    /**
     * 获取选中的文本
     * @return 选中的文本
     */
    public String getSelectedText() {
        return selectedText;
    }

    /**
     * 设置选中的文本
     * @param selectedText 选中的文本
     */
    public void setSelectedText(String selectedText) {

        int index = -1;
        for (int i = 0; i < texts.length; i++) {
            String text = texts[i];
            if (selectedText.contentEquals(text)) {
                index = i;
                break;
            }
        }
        if (index != -1 ) {
            this.selectedText = selectedText;

            for (int x = 0; x < getChildCount(); x++) {
                View childView = getChildAt(x);
                if (childView instanceof Button) {
                    Button childButton = (Button)childView;
                    if (getChildAt(index) == childButton) {
                        childButton.setSelected(true);
                    } else {
                        childButton.setSelected(false);
                    }
                }
            }
        }
    }
}
