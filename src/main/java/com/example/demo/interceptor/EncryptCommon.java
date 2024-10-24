package com.example.demo.interceptor;

import cn.hutool.crypto.SecureUtil;

public class EncryptCommon {

    private static final String KEY = "1qaz@WSX3edc$RFV";

    public static String encrypt(String data) {
        return SecureUtil.aes(KEY.getBytes()).encryptHex(data);
    }

    public static String decrypt(String data) {
        return SecureUtil.aes(KEY.getBytes()).decryptStr(data);
    }

    public static boolean matchClass(String className) {
        return "com.example.demo.entity.User".equals(className);
    }

    public static boolean matchField(String fieldName) {
        return "password".equals(fieldName);
    }
}
