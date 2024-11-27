package com.example.demo.sdk;

import com.example.demo.sdk.dto.AuthRequest;
import com.example.demo.sdk.dto.AuthResponse;

public class BasePlatformServiceImpl implements IPlatformService {

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
