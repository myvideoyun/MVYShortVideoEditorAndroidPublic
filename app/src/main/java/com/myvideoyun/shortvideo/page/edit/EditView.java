package com.myvideoyun.shortvideo.page.edit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.support.constraint.ConstraintLayout;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.myvideoyun.shortvideo.R;
import com.myvideoyun.shortvideo.customUI.MotionLayout;
import com.myvideoyun.shortvideo.customUI.RangeBar;
import com.myvideoyun.shortvideo.page.record.model.MediaInfoModel;
import com.myvideoyun.shortvideo.page.record.model.StyleModel;
import com.myvideoyun.shortvideo.page.record.view.StylePlane;
import com.myvideoyun.shortvideo.MVYPreviewView;
import com.myvideoyun.shortvideo.tools.ScreenTools;

/**
 * Created by 汪洋 on 2019/1/31.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class EditView extends FrameLayout implements View.OnClickListener, StylePlane.StylePlaneOnClickItemListener {

    View containerView;
    MVYPreviewView preview;
    ImageButton backBt;
    Button nextPageBt;
    CheckBox originalAudioBt;
    Button musicBt;
    Button cutAudioBt;
    Button audioBt;
    Button videoEffectBt;
    Button stickerBt;
    EditCallback callback;
    SeekBar originalBar;
    SeekBar musicBar;
    private int max = 100;
    private ConstraintLayout editVolumeCl;
    private View blankCl;
    private ConstraintLayout rootCl;
    private View mCurrentShowDialogView;
    private StylePlane stickerStylePlane;
    private EditText inputView;
    private TextView orangeBulbView;
    private TextView greenBulbView;
    private TextView blueBulbView;
    private Button subtitleBt;
    private View subtitleCl;
    private View rangeBarCl;
    private Button coverBt;
    private Button duetBtn;
    MotionLayout stickerMl;
    RangeBar rangeBar;

    public EditView(Context context) {
        super(context);

        setupView();
    }

    private void setupView() {
        containerView = inflate(getContext(), R.layout.activity_edit, this);

        preview = findViewById(R.id.edit_preview);
        blankCl = findViewById(R.id.blankCl);
        rootCl = findViewById(R.id.edit_root_cl);
        backBt = findViewById(R.id.edit_back_bt);
        nextPageBt = findViewById(R.id.edit_next_page_bt);
        originalAudioBt = findViewById(R.id.edit_original_audio_bt);
        musicBt = findViewById(R.id.edit_music_bt);
        cutAudioBt = findViewById(R.id.edit_cut_audio_bt);
        audioBt = findViewById(R.id.edit_audio_bt);
        videoEffectBt = findViewById(R.id.edit_video_effect_bt);
        stickerBt = findViewById(R.id.edit_sticker_bt);
        subtitleBt = findViewById(R.id.edit_subtitle_bt);
        coverBt = findViewById(R.id.edit_cover_bt);
        duetBtn = findViewById(R.id.edit_in_step_shoot);
        editVolumeCl = findViewById(R.id.edit_volume_cl);
        stickerStylePlane = findViewById(R.id.stickerStylePlane);
        subtitleCl = findViewById(R.id.edit_subtitle_cl);
        rangeBarCl = findViewById(R.id.edit_range_bar_cl);

        inputView = findViewById(R.id.editView);
        orangeBulbView = findViewById(R.id.orangeBulbView);
        greenBulbView = findViewById(R.id.greenBulbView);
        blueBulbView = findViewById(R.id.blueBulbView);
        rangeBar = findViewById(R.id.range_bar);

        stickerMl = findViewById(R.id.sticker_ml);

        orangeBulbView.setOnClickListener(this);
        greenBulbView.setOnClickListener(this);
        blueBulbView.setOnClickListener(this);

        stickerStylePlane.setOnClickItemListener(this);
        stickerStylePlane.setOutskirtsHide(false);

        duetBtn.setOnClickListener(this);

        backBt.setImageResource(R.drawable.selector_record_back_button);
        backBt.setBackgroundColor(Color.TRANSPARENT);

        blankCl.setOnClickListener(this);
        coverBt.setOnClickListener(this);

        nextPageBt.setText("下一步");
        nextPageBt.setTextColor(Color.WHITE);
        nextPageBt.setBackgroundColor(Color.TRANSPARENT);
        nextPageBt.setOnClickListener(this);

        originalAudioBt.setText("原声开");
        originalAudioBt.setPadding(0,0,0,0);
//        originalAudioBt.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.selector_edit_original_audio_button), null, null);
        originalAudioBt.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.f, getResources().getDisplayMetrics()));
        originalAudioBt.setBackgroundColor(Color.TRANSPARENT);
        originalAudioBt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    buttonView.setText("原声开");
                } else {
                    buttonView.setText("原声关");
                }
                if (callback != null) callback.switchAudioToOriginal(isChecked);
            }
        });
//        originalAudioBt.setOnClickListener(this);

        musicBt.setText("音乐");
        musicBt.setPadding(0,0,0,0);
        musicBt.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.btn_music_n), null, null);
        musicBt.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.f, getResources().getDisplayMetrics()));
        musicBt.setBackgroundColor(Color.TRANSPARENT);
        musicBt.setOnClickListener(this);

        cutAudioBt.setText("剪音乐");
        cutAudioBt.setPadding(0,0,0,0);
        cutAudioBt.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.btn_clip_n), null, null);
        cutAudioBt.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.f, getResources().getDisplayMetrics()));
        cutAudioBt.setBackgroundColor(Color.TRANSPARENT);
        cutAudioBt.setOnClickListener(this);

        audioBt.setText("音量");
        audioBt.setPadding(0,0,0,0);
        audioBt.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.btn_voice_n), null, null);
        audioBt.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.f, getResources().getDisplayMetrics()));
        audioBt.setBackgroundColor(Color.TRANSPARENT);
        audioBt.setOnClickListener(this);

        videoEffectBt.setText("特效");
        videoEffectBt.setPadding(0,0,0,0);
        videoEffectBt.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.btn_special_effects_n), null, null);
        videoEffectBt.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.f, getResources().getDisplayMetrics()));
        videoEffectBt.setBackgroundColor(Color.TRANSPARENT);
        videoEffectBt.setOnClickListener(this);

        stickerBt.setText("贴纸");
        stickerBt.setPadding(0,0,0,0);
        stickerBt.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.btn_watermark_n), null, null);
        stickerBt.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.f, getResources().getDisplayMetrics()));
        stickerBt.setBackgroundColor(Color.TRANSPARENT);
        stickerBt.setOnClickListener(this);

        subtitleBt.setOnClickListener(this);

        originalBar = findViewById(R.id.edit_original_volume_pb);
        originalBar.setMax(max);
        originalBar.setProgress(50);
        originalBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (callback != null) callback.setOriginalVolume(max, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        musicBar = findViewById(R.id.edit_music_volume_pb);
        musicBar.setMax(max);
        musicBar.setProgress(50);
        musicBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (callback != null) callback.setMusicVolume(max, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v == cutAudioBt){
            if (callback != null){
                callback.cutMusic();
            }
        } else if (v == blankCl){
            // 交换显示
            showDialogView(false, mCurrentShowDialogView);
        } else if (v == musicBt){
            if (callback != null){
                callback.chooseMusic();
            }
        } else if (v == coverBt){
            if (callback != null){
                callback.choiseCover();
            }
        }  else if (v == duetBtn){
            if (callback != null){
                callback.duetShoot();
            }
        } else if (v == audioBt){
//            int visibility = editVolumeCl.getVisibility();
//            if (visibility == View.VISIBLE){
//                editVolumeCl.setVisibility(View.GONE);
//            } else {
//                editVolumeCl.setVisibility(View.VISIBLE);
//            }
            showDialogView(true, editVolumeCl);
        } else if (v == stickerBt){
            showDialogView(true, stickerStylePlane);
//            if (callback != null) callback.addSticker();
        } else if (v == subtitleBt){
            showDialogView(true, subtitleCl);
        } else if (v == orangeBulbView){
            orangeBulbView.setText(inputView.getText().toString());
            orangeBulbView.setDrawingCacheEnabled(true);
            orangeBulbView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            showDialogView(false, mCurrentShowDialogView);
            if (callback != null) callback.addSubtitle(Bitmap.createBitmap(orangeBulbView.getDrawingCache()));
        } else if (v == greenBulbView){
            greenBulbView.setDrawingCacheEnabled(true);
            greenBulbView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            greenBulbView.setText(inputView.getText().toString());
            showDialogView(false, mCurrentShowDialogView);
            if (callback != null) callback.addSubtitle(Bitmap.createBitmap(greenBulbView.getDrawingCache()));
        } else if (v == blueBulbView){
            blueBulbView.setDrawingCacheEnabled(true);
            blueBulbView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            blueBulbView.setText(inputView.getText().toString());
            showDialogView(false, mCurrentShowDialogView);
            if (callback != null) callback.addSubtitle( Bitmap.createBitmap(blueBulbView.getDrawingCache()));
        } else if (v == nextPageBt) {
            if (callback != null) callback.goNextOutput();
        } else if (v == videoEffectBt) {
            if (callback != null) callback.clickEffect();
        }
    }

    /**
     * 控制底部浮窗是否显示，同时控制除底部浮窗外的其他控件（排除预览）
     * @param isShow
     */
    private void showDialogView(boolean isShow, View currentShowDialogView) {
        int visibility = isShow ? INVISIBLE : VISIBLE;
        View temp;
        int childCount = rootCl.getChildCount();
        for (int i= 0; i < childCount; i++){
            temp = rootCl.getChildAt(i);
            if (temp != blankCl && temp != preview){
                temp.setVisibility(visibility);
            }
        }

        int blankVisibility = isShow ? VISIBLE : INVISIBLE;
        blankCl.setVisibility(blankVisibility);
        // todo 还有一个当前选中的操作对应的 view
        // currentShowDialogView
        if (currentShowDialogView != null) currentShowDialogView.setVisibility(blankVisibility);
        this.mCurrentShowDialogView = currentShowDialogView;
    }

    @Override
    public void onClickItem(int position, StyleModel model) {
        if (callback != null) callback.addSticker(model);
        showDialogView(false, mCurrentShowDialogView);
    }

    public View addViewToMotionLayout(String path) {
        return addViewToMotionLayout(path, 200);
    }

    public View addViewToMotionLayout(String path, int side){
        TextView view = new TextView(getContext());
        ViewGroup.LayoutParams params = new MotionLayout.LayoutParams(side, side, 0.5f, 0.5f);
//        ViewGroup.LayoutParams params = new MotionLayout.LayoutParams(200, 200, 0.5f, 0.5f);
//        view.setBackgroundColor(0x99839239);
//        view.setBackground(new BitmapDrawable(BitmapFactory.decodeFile(path)));
        view.setLayoutParams(params);
        stickerMl.addView(view, path);
        return view;
    }

    public void showRangeBar(boolean isShow) {
        showDialogView(isShow, rangeBarCl);
    }

    /**
     * 设置贴纸view 大小跟视频比例
     * @param model
     */
    public void setStickerMappingView(Activity activity, MediaInfoModel model) {
        // 获取视频方向
        try {
            MediaMetadataRetriever retr = new MediaMetadataRetriever();
            retr.setDataSource(model.videoPath);
            String rotation = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            int width = Integer.parseInt(retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int height = Integer.parseInt(retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));

            Point screen = ScreenTools.getScreen(activity);
            long viewHeight = height * screen.x / width;

            if ("270".equals(rotation) || "90".equals(rotation)){
                viewHeight = (screen.x * width / height);
            }

//            Log.e(TAG, "视频大小：width = " + width + "， height=" + height + ", rotation = " + rotation);
//            Log.e(TAG, "设置贴纸容器大小：width = " + screen.x + "， height=" + viewHeight);
            ViewGroup.LayoutParams layoutParams = stickerMl.getLayoutParams();
            layoutParams.width = screen.x;
            layoutParams.height = (int) viewHeight;
//            stickerMl.setBackgroundColor(0x77987698);
            stickerMl.setLayoutParams(layoutParams);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

    }
}
