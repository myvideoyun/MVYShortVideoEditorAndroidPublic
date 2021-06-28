package com.myvideoyun.shortvideo.page.record.model;

import com.myvideoyun.shortvideo.page.record.view.StylePlane;

/**
 * Created by 汪洋 on 2019/2/11.
 * Copyright © 2019年 myvideoyun. All rights reserved.
 */
public class StyleModel {
    public String thumbnail;
    public String text;
    public String path;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StylePlane && obj.toString().equals(this.toString())) {
            return true;
        }
        return super.equals(obj);
    }
}
