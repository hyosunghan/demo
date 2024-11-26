package com.example.demo.sdk.dto;

import lombok.Data;

@Data
public class AuthRequest extends BaseRequest {

    /**
     * 授权码
     */
    private String code;

    /**
     * 刷新令牌
     */
    private String refreshToken;
}
