package com.myvideoyun.shortvideo.page.addpic;

import com.myvideoyun.shortvideo.page.record.model.StyleModel;
import com.myvideoyun.shortvideo.page.record.model.StyleModel;

/**
 * Created by yangshunfa on 2019/3/9.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public interface PictureViewCallback {
    void backwards();
    void finishEdit();

    void cutChoiseStartPosition(float position);

    void cutChoiseEndPosition(float position);

    void cutFinish(float start, float end);

    void cutChoosing();

    void addStickerToVideo(StyleModel model);
}
