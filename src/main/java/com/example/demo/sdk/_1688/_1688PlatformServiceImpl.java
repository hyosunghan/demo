package com.example.demo.sdk._1688;

import com.example.demo.sdk.AbstractPlatformServiceImpl;
import com.example.demo.sdk.PlatformProperties;
import com.example.demo.sdk.dto.AuthRequest;
import com.example.demo.sdk.dto.AuthResponse;

public class _1688PlatformServiceImpl extends AbstractPlatformServiceImpl {

    private final PlatformProperties platformProperties;

    public _1688PlatformServiceImpl(PlatformProperties platformProperties) {
        this.platformProperties = platformProperties;
    }

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
