package com.example.demo.interceptor;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.crypto.SecureUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EncryptCommon {

    public static final Map<String, Set<String>> CUSTOM_PROPERTY_MAP = new HashMap<>();

    private static final String KEY = "1qaz@WSX3edc$RFV";

    public static String encrypt(String data) {
        return SecureUtil.aes(KEY.getBytes()).encryptHex(data);
    }

    public static String decrypt(String data) {
        return SecureUtil.aes(KEY.getBytes()).decryptStr(data);
    }

    public static String desensitize(String fieldName, String value) {
        if ("phoneNumber".equals(fieldName)) {
            return DesensitizedUtil.mobilePhone(value);
        }
        return value;
    }

    public static boolean matchClass(String className) {
        return CUSTOM_PROPERTY_MAP.containsKey(className);
    }

    public static boolean matchField(String className, String fieldName) {
        return CUSTOM_PROPERTY_MAP.get(className).contains(fieldName);
    }
}
