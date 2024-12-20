package com.example.demo.sdk;

import com.example.demo.sdk._1688._1688PlatformServiceImpl;
import com.example.demo.sdk.dto.AuthRequest;
import com.example.demo.sdk.dto.AuthResponse;
import com.example.demo.sdk.dto.PlatformEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PlatformServiceImpl implements IPlatformService {

    private final Map<PlatformEnum, IPlatformService> platformServiceMap;

    public PlatformServiceImpl(PlatformProperties platformProperties) {
        this.platformServiceMap = new HashMap<>();
        platformServiceMap.put(PlatformEnum._1688, new _1688PlatformServiceImpl(platformProperties));
    }

    @Override
    public AuthResponse refreshAccessToken(AuthRequest authRequest) {
        return platformServiceMap.get(authRequest.getPlatform()).refreshAccessToken(authRequest);
    }
}
