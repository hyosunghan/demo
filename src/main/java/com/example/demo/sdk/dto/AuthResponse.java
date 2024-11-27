package com.example.demo.sdk.dto;

import lombok.Data;

@Data
public class AuthResponse {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 有效时间
     */
    private String expiresIn;

    /**
     * 用户ID
     */
    private String userId;
}
