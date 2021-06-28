package com.myvideoyun.shortvideo.GPUImage;

import java.util.ArrayList;

/**
 * Created by 汪洋 on 2018/12/8.
 * Copyright © 2018年 myvideoyun. All rights reserved.
 */
public class MVYGPUImageOutput {

    private ArrayList<MVYGPUImageInput> targets = new ArrayList();

    protected ArrayList<MVYGPUImageInput> getTargets() {
        return targets;
    }

    public void addTarget(MVYGPUImageInput newTarget) {
        if (targets.contains(newTarget)) {
            return;
        }

        targets.add(newTarget);
    }

    public void removeTarget(MVYGPUImageInput targetToRemove) {
        if (!targets.contains(targetToRemove)) {
            return;
        }

        targets.remove(targetToRemove);
    }

    public void removeAllTargets() {
        targets.clear();
    }
}