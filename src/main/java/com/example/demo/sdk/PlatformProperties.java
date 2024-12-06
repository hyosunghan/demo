package com.example.demo.sdk;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class PlatformProperties {

    @Value("${platform.1688.app-id}")
    private String _1688AppId;

    @Value("${platform.1688.app-secret}")
    private String _1688AppSecret;

}
