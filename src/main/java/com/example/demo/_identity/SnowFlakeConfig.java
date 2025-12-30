package com.example.demo._identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowFlakeConfig {

    @Value("${system.node-id:-1}")
    private int machineNumber;
    
    @Bean
    public SnowFlakeIdentity snowFlakeIdentity() {
        SnowFlakeIdentity.machineNumber = machineNumber;
        return SnowFlakeIdentity.getInstance();
    }
}