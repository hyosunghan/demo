package com.example.demo.sdk.dto;

import lombok.Data;

@Data
public class BaseRequest {

    /**
     * 平台类型
     */
    private PlatformEnum platform;

    /**
     * 令牌
     */
    private String accessToken;

    /**
     * 用户ID
     */
    private String userId;
}
