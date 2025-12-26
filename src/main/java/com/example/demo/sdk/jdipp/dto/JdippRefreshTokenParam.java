package com.example.demo.sdk.jdipp.dto;

import lombok.Data;

@Data
public class JdippRefreshTokenParam {

    private String accessToken;

    private String refreshToken;

    private Integer code;

    private String message;

    private String expireIn;

    private String time;

    private String refreshExpireIn;

    private String pin;

    private String tokenType;

    private String traceId;
}
