package com.myvideoyun.shortvideo.tools;

import java.text.SimpleDateFormat;

public class StringUtils {

    private static SimpleDateFormat format = new SimpleDateFormat("mm:ss.SSS");
    /**
     * 毫秒转成 00:00:00.000格式
     * @param ms
     * @return
     */
    public static String convertToSecond(int ms){
        return "00:" + StringUtils.format.format(ms);
    }

}
