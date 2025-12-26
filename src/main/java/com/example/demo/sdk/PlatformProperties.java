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

    @Value("${platform.1688.app-token}")
    private String _1688AppToken;

    @Value("${platform.1688.product-collection-ids}")
    private String _1688ProductCollectionIds;

    @Value("${platform.1688.rate-profit}")
    private String _1688RateProfit;

    @Value("${platform.1688.fix-profit}")
    private String _1688FixProfit;

    @Value("${platform.1688.default-lang}")
    private String _1688DefaultLang;

    @Value("${platform.1688.default-currency}")
    private String _1688DefaultCurrency;

    //-----------------------------------------

    @Value("${platform.jdipp.app-id}")
    private String jdippAppId;

    @Value("${platform.jdipp.app-secret}")
    private String jdippAppSecret;

    @Value("${platform.jdipp.rate-profit}")
    private String jdippRateProfit;

    @Value("${platform.jdipp.fix-profit}")
    private String jdippFixProfit;

}
