package com.example.demo.sdk;

import com.example.demo.sdk.dto.AuthRequest;
import com.example.demo.sdk.dto.AuthResponse;

public interface IPlatformService {

    AuthResponse refreshAccessToken(AuthRequest baseRequest);
}
