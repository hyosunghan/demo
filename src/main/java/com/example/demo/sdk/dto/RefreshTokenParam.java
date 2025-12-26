package com.example.demo.sdk.dto;

import lombok.Data;

@Data
public class RefreshTokenParam extends BaseParam {

    private String refreshToken;

    private String token;

    private Long tokenExpire;

    private Long refreshTokenExpire;
}
