package com.example.demo.sdk._1688;

import com.example.demo.sdk.AbstractPlatformServiceImpl;
import com.example.demo.sdk.dto.AuthRequest;
import com.example.demo.sdk.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Value;

public class _1688PlatformServiceImpl extends AbstractPlatformServiceImpl {

    @Value("${platform.1688.app-id}")
    private String appId;

    @Value("${platform.1688.app-secret}")
    private String appSecret;

    @Override
    public AuthResponse refreshAccessToken(AuthRequest baseRequest) {
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken("aaa");
        authResponse.setRefreshToken("bbb");
        authResponse.setUserId("111");
        authResponse.setExpiresIn("222");
        return authResponse;
    }
}
