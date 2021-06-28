package com.myvideoyun.shortvideo.recordTool;

public class MVYAudioMixer {

    public static short mix(short inputAudio_1, short inputAudio_2, short volume_1, short volume_2) {

        int result = (inputAudio_1 & 0xFFFF) * volume_1 + (inputAudio_2 & 0xFFFF) * volume_2;

        result = result >> 12;

        if (((result >> 15) ^ (result >> 31)) > 0)
            result = 0x7FFF ^ (result>>31);

        return (short) (result & 0xFFFF);
    }
}
