package com.myvideoyun.shortvideo.page.cover;

/**
 * Created by yangshunfa on 2019/3/9.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public interface CoverSelectionViewCallback {
    void backwards();
    void finishEdit();

    /**
     * 触摸控件
     * @param position
     */
    void cutChoiseStart(float position);

    /**
     * 拖拽完成
     * @param start
     * @param end
     */
    void cutFinish(float start, float end);

    /**
     * 拖拽中
     * @param start
     * @param end
     */
    void dragging(float start, float end);
}
