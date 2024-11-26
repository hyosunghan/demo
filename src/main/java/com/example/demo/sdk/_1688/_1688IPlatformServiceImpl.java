package com.example.demo.sdk._1688;

import com.example.demo.sdk.IPlatformService;
import com.example.demo.sdk.dto.AuthRequest;
import com.example.demo.sdk.dto.AuthResponse;
import com.example.demo.sdk.dto.BaseRequest;
import org.springframework.beans.factory.annotation.Value;

public class _1688IPlatformServiceImpl implements IPlatformService {

    @Value("${platform.1688.app-id}")
    private String appId;

    @Value("${platform.1688.app-secret}")
    private String appSecret;

    @Value("${platform.1688.redirect-url}")
    private String redirectUrl;

    @Override
    public String getAuthUrl(BaseRequest baseRequest) {
        return "https://auth.1688.com/oauth/authorize?client_id=" + appId + "&site=1688&redirect_uri=" + redirectUrl
                + "&state=" + System.currentTimeMillis();
    }

    @Override
    public AuthResponse getAccessToken(AuthRequest authRequest) {
        return null;
    }

    @Override
    public AuthResponse refreshAccessToken(AuthRequest baseRequest) {
        return null;
    }
}
