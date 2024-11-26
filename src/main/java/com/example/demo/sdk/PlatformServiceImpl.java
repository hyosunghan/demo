package com.example.demo.sdk;

import com.example.demo.sdk._1688._1688IPlatformServiceImpl;
import com.example.demo.sdk.dto.AuthRequest;
import com.example.demo.sdk.dto.AuthResponse;
import com.example.demo.sdk.dto.BaseRequest;
import com.example.demo.sdk.dto.PlatformEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PlatformServiceImpl implements IPlatformService {

    private static final Map<PlatformEnum, IPlatformService> platformServiceMap = new HashMap<>();

    static {
        platformServiceMap.put(PlatformEnum._1688, new _1688IPlatformServiceImpl());
    }

    @Override
    public String getAuthUrl(BaseRequest baseRequest) {
        return platformServiceMap.get(baseRequest.getPlatform()).getAuthUrl(baseRequest);
    }

    @Override
    public AuthResponse getAccessToken(AuthRequest authRequest) {
        return platformServiceMap.get(authRequest.getPlatform()).getAccessToken(authRequest);
    }

    @Override
    public AuthResponse refreshAccessToken(AuthRequest authRequest) {
        return platformServiceMap.get(authRequest.getPlatform()).refreshAccessToken(authRequest);
    }
}
