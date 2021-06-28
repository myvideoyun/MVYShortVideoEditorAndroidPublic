package com.myvideoyun.shortvideo.page.cut;

public interface CutViewCallback {
    void backwards();
    void next();

    void cutChoiseStartPosition(float position);

    void cutChoiseEndPosition(float position);

    void cutFinish(float start, float end);

    void cutChoosing();
}
