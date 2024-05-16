package com.sunriver.arvhiceSystem.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;

/*
    改变文件名编码格式，避免中文乱码
 */
public class DownLoadUtils {

    public static String getFileName(String agent, String filename) throws UnsupportedEncodingException {
        if (agent.contains("MSIE")) {
            // IE浏览器
            filename = URLEncoder.encode(filename, "utf-8");
            filename = filename.replace("+", " ");
        } else if (agent.contains("Firefox")) {
            // 火狐浏览器
            Base64.Encoder base64Encoder = Base64.getEncoder();
            filename = "=?utf-8?B?" + base64Encoder.encodeToString(filename.getBytes("utf-8")) + "?=";
        } else {
            // 其它浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        }
        return filename;
    }
}

