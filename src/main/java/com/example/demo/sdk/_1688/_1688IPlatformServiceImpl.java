package com.example.demo.sdk._1688;

import com.example.demo.sdk.BasePlatformServiceImpl;
import org.springframework.beans.factory.annotation.Value;

public class _1688IPlatformServiceImpl extends BasePlatformServiceImpl {

    @Value("${platform.1688.app-id}")
    private String appId;

    @Value("${platform.1688.app-secret}")
    private String appSecret;

}
