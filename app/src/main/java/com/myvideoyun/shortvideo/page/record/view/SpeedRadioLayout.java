package com.myvideoyun.shortvideo.page.record.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.myvideoyun.shortvideo.R;

import java.util.ArrayList;

/**
 * Created by 汪洋 on 2019/2/11.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class SpeedRadioLayout extends LinearLayout implements View.OnClickListener {

    private String[] texts = new String[]{"极慢", "慢", "标准", "快", "极快"};
    private ArrayList<Button> buttons = new ArrayList<>();

    private String selectedText;

    private OnClickTabListener listener = null;

    public SpeedRadioLayout(Context context) {
        super(context);
        setupView();
    }

    public SpeedRadioLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupView();
    }

    public SpeedRadioLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupView();
    }

    private void setupView() {
        setBackgroundResource(R.drawable.shape_white_border);

        for (int i=0; i<texts.length; i++) {
            String text = texts[i];

            Button button = new Button(getContext());
            button.setText(text);
            button.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_selected},{}},
                    new int[]{Color.argb(255, 254, 112, 68), Color.WHITE}));
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            button.setBackgroundResource(R.drawable.selector_speed_radio_layout);
            button.setOnClickListener(this);

            buttons.add(button);
            addView(button);
        }

        for (int i=0; i<buttons.size(); i++) {
            Button button = buttons.get(i);
            button.setPadding(0,0,0,0);

            LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.weight = 1;
            button.setLayoutParams(params);
        }
    }

    @Override
    public void onClick(View v) {
        for (Button button : buttons) {
            if (v == button) {
                button.setSelected(true);
                selectedText = button.getText().toString();
                if (listener != null){
                    listener.onClick(buttons.indexOf(button));
                }
            } else {
                button.setSelected(false);
            }
        }
    }

    public void setSelectedText(String selectedText) {
        for (int i=0; i<texts.length; i++){
            String text = texts[i];

            if (text.equals(selectedText)) {
                this.selectedText = selectedText;
                onClick(buttons.get(i));
            }
        }
    }


    public void setOnClickTabListener(OnClickTabListener l){
        this.listener = l;
    }

    public interface OnClickTabListener{
        void onClick(int index);
    }
}
