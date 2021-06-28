package com.myvideoyun.shortvideo.recordTool;

import java.nio.ByteBuffer;

public interface MVYAudioRecordListener {
    void audioRecordOutput(ByteBuffer byteBuffer, long timestamp);
}
