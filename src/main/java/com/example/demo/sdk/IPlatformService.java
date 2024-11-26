package com.example.demo.sdk;

import com.example.demo.sdk.dto.AuthRequest;
import com.example.demo.sdk.dto.AuthResponse;
import com.example.demo.sdk.dto.BaseRequest;

public interface IPlatformService {

    String getAuthUrl(BaseRequest baseRequest);

    AuthResponse getAccessToken(AuthRequest baseRequest);

    AuthResponse refreshAccessToken(AuthRequest baseRequest);
}
