package com.myvideoyun.shortvideo.page.cutmusic;
/**
 * Created by yangshunfa on 2019/3/5.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public interface CutViewCallback {
    void backwards();
    void next();

    void cutChoiseStart(float position);

    /**
     * 完成拖拽
     * @param start
     * @param end
     */
    void cutChoiseEnd(float start, float end);

    /**
     * 拖拽中
     * @param start
     * @param end
     */
    void cutDragging(float start, float end);

}
