package com.myvideoyun.shortvideo.page.effect;

public interface EffectViewCallback {

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
