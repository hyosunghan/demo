package com.example.demo.sdk;

import com.example.demo.sdk.dto.AuthRequest;
import com.example.demo.sdk.dto.AuthResponse;

public abstract class AbstractPlatformServiceImpl implements IPlatformService {

    @Override
    public AuthResponse refreshAccessToken(AuthRequest baseRequest) {
        return null;
    }
}
