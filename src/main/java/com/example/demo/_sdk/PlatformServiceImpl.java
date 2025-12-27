package com.example.demo._sdk;

import com.example.demo._sdk._1688._1688PlatformServiceImpl;
import com.example.demo._sdk.dto.*;
import com.example.demo._sdk.jdipp.JdippPlatformServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PlatformServiceImpl implements IPlatformService {

    private final Map<PlatformEnum, IPlatformService> platformServiceMap;

    public PlatformServiceImpl(PlatformProperties platformProperties, StringRedisTemplate stringRedisTemplate) {
        this.platformServiceMap = new HashMap<>();
        platformServiceMap.put(PlatformEnum._1688, new _1688PlatformServiceImpl(platformProperties));
        platformServiceMap.put(PlatformEnum.JDIPP, new JdippPlatformServiceImpl(platformProperties, stringRedisTemplate));
    }

    @Override
    public RefreshTokenParam refreshToken(RefreshTokenParam refreshTokenParam) {
        return platformServiceMap.get(refreshTokenParam.getPlatform()).refreshToken(refreshTokenParam);
    }

    @Override
    public TopProductsResult getTopProducts(TopProductsParam topProductsParam) {
        return platformServiceMap.get(topProductsParam.getPlatform()).getTopProducts(topProductsParam);
    }

    @Override
    public ProductParam getProductDetail(ProductDetailParam productDetailParam) {
        return platformServiceMap.get(productDetailParam.getPlatform()).getProductDetail(productDetailParam);
    }

    @Override
    public PreviewOrderResult previewOrder(CreateOrderParam createOrderParam) {
        return platformServiceMap.get(createOrderParam.getPlatform()).previewOrder(createOrderParam);
    }

    @Override
    public CreateOrderResult createOrder(CreateOrderParam createOrderParam) {
        return platformServiceMap.get(createOrderParam.getPlatform()).createOrder(createOrderParam);
    }

    @Override
    public String parseAddressCode(AddressCodeParseParam addressCodeParseParam) {
        return platformServiceMap.get(addressCodeParseParam.getPlatform()).parseAddressCode(addressCodeParseParam);
    }

    @Override
    public PayUrlResult getPayUrl(PayUrlParam payUrlParam) {
        return platformServiceMap.get(payUrlParam.getPlatform()).getPayUrl(payUrlParam);
    }

    @Override
    public ConfirmReceiveGoodsResult confirmReceiveGoods(OrderParam orderParam) {
        return platformServiceMap.get(orderParam.getPlatform()).confirmReceiveGoods(orderParam);
    }

    @Override
    public List<TrackingInfoResult> getTrackingInfo(OrderParam orderParam) {
        return platformServiceMap.get(orderParam.getPlatform()).getTrackingInfo(orderParam);
    }
}
