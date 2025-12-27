package com.example.demo._sdk.dto;

import lombok.Data;

import java.util.Map;

@Data
public class BaseParam {

    /**
     * 平台
     */
    private PlatformEnum platform;

    /**
     * 语言
     */
    private String language;

    /**
     * 系统语言映射
     */
    private Map<String, Integer> langMap;

    /**
     * 币种汇率映射
     */
    private Map<String, Double> currencyMap;

    /**
     * 时间
     */
    private Long dateTime;
}
