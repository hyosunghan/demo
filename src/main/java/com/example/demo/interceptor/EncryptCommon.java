package com.example.demo.interceptor;

public class EncryptCommon {

    public static String encrypt(String data) {
        return data + "****";
    }

    public static String decrypt(String data) {
        return data.replace("****", "");
    }

    public static boolean matchClass(String className) {
        return "com.example.demo.entity.User".equals(className);
    }

    public static boolean matchField(String fieldName) {
        return "password".equals(fieldName);
    }
}
