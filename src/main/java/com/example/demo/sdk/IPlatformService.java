package com.example.demo.sdk;

import com.example.demo.sdk.dto.*;

import java.util.List;

public interface IPlatformService {

    String PLATFORM_TOKEN_CACHE_PREFIX = "PLATFORM_TOKEN_CACHE:";

    /**
     * 刷新令牌
     *
     * @param refreshTokenParam
     * @return
     */
    RefreshTokenParam refreshToken(RefreshTokenParam refreshTokenParam);

    /**
     * 获取热销商品列表
     *
     * @param topProductsParam
     * @return
     */
    TopProductsResult getTopProducts(TopProductsParam topProductsParam);

    /**
     * 查询商品详情
     *
     * @param productDetailParam
     * @return
     */
    ProductParam getProductDetail(ProductDetailParam productDetailParam);

    /**
     * 订单预览
     *
     * @param createOrderParam
     * @return
     */
    PreviewOrderResult previewOrder(CreateOrderParam createOrderParam);

    /**
     * 创建订单
     *
     * @param createOrderParam
     * @return
     */
    CreateOrderResult createOrder(CreateOrderParam createOrderParam);

    /**
     * 解析地区码
     *
     * @param addressCodeParseParam
     * @return
     */
    String parseAddressCode(AddressCodeParseParam addressCodeParseParam);

    /**
     * 获取支付链接
     *
     * @param payUrlParam
     * @return
     */
    PayUrlResult getPayUrl(PayUrlParam payUrlParam);

    /**
     * 获取物流信息
     *
     * @param orderParam
     * @return
     */
    List<TrackingInfoResult> getTrackingInfo(OrderParam orderParam);

    /**
     * 确认收货
     *
     * @param orderParam
     * @return
     */
    ConfirmReceiveGoodsResult confirmReceiveGoods(OrderParam orderParam);
}
