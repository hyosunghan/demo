package com.example.demo._sdk.jdipp;

import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONUtil;
import com.example.demo._sdk.AbstractPlatformServiceImpl;
import com.example.demo._sdk.PlatformProperties;
import com.example.demo._sdk.dto.*;
import com.example.demo._sdk.jdipp.dto.*;
import com.example.demo.utils.RestUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JdippPlatformServiceImpl extends AbstractPlatformServiceImpl {

    private final PlatformProperties platformProperties;

    private final StringRedisTemplate stringRedisTemplate;

    private final String format = "json";

    private final String version = "V1";

    private final String domain = "https://api.joybuy.com/router";

    private final String dateFormat = "yyyy-MM-dd HH:mm:ss ZZZ";

    private final String defaultLanguage = "en";

    private final Integer defaultSoldNum = 9999;

    public JdippPlatformServiceImpl(PlatformProperties platformProperties, StringRedisTemplate stringRedisTemplate) {
        this.platformProperties = platformProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public RefreshTokenParam refreshToken(RefreshTokenParam refreshTokenParam) {
        String url = "https://api.joybuy.com/token?grantType=refresh_token&appKey=" + platformProperties.getJdippAppId()
                + "&appSecret=" + platformProperties.getJdippAppSecret()
                + "&refreshToken=" + refreshTokenParam.getRefreshToken();
        JdippRefreshTokenParam result = RestUtil.post(url, null, JdippRefreshTokenParam.class, new HashMap<>());
        refreshTokenParam.setToken(result.getAccessToken());
        refreshTokenParam.setRefreshToken(result.getRefreshToken());
        refreshTokenParam.setTokenExpire(DateUtil.parse(result.getExpireIn(), dateFormat).getTime());
        refreshTokenParam.setRefreshTokenExpire(DateUtil.parse(result.getRefreshExpireIn(), dateFormat).getTime());
        return refreshTokenParam;
    }

    @Override
    public TopProductsResult getTopProducts(TopProductsParam topProductsParam) {
        TopProductsResult result = new TopProductsResult();
        JdippProductListParam param = new JdippProductListParam();
        param.setPageSize(topProductsParam.getLimit());
        param.setLanguage(defaultLanguage);
        param.setNextCursor(topProductsParam.getCursor());
        URI uri = constructUri(param, "jdipp.product.list");
        JdippProductListResult result1 = RestUtil.post(uri, null, JdippProductListResult.class);
        if (result1.getModel() == null || result1.getModel().getData() == null) {
            return result;
        }
        Map<String, Double> currencyMap = topProductsParam.getCurrencyMap();
        result.setCursor(result1.getModel().getNextCursor());
        result.setTotal(result1.getModel().getTotal());
        List<TopProductsResult.TopProductListResultItem> list = result1.getModel().getData().parallelStream().map(s -> {
            TopProductsResult.TopProductListResultItem i = new TopProductsResult.TopProductListResultItem();
            i.setSoldOut(defaultSoldNum);
            i.setImgUrl(s.getSkuImageUrl());
            i.setItemId(s.getSku());
            JdippProductDetailParam param1 = new JdippProductDetailParam();
            param1.setSku(Long.valueOf(s.getSku()));
            param1.setLanguage(defaultLanguage);
            URI uri1 = constructUri(param1, "jdipp.product.get_detail_bysku");
            JdippProductDetailResult result2 = RestUtil.post(uri1, null, JdippProductDetailResult.class);
            if (result2 == null || result2.getModel() == null || result2.getModel().getData() == null) {
                return i;
            }
            JdippProductDetailResult.Sku sku = result2.getModel().getData();
            if (sku.getPriceVo() != null) {
                Double currencyRate = currencyMap.get(sku.getPriceVo().getCurrency());
                i.setPrice(getCurrentPrice(sku.getPriceVo().getPrice().toString(), currencyRate,
                        platformProperties.getJdippFixProfit(), platformProperties.getJdippRateProfit()));
            }
            i.setTitle(sku.getSkuName());
            return i;
        }).collect(Collectors.toList());
        result.setTopProductListResultItemList(list);
        return result;
    }

    @Override
    public ProductParam getProductDetail(ProductDetailParam productDetailParam) {
        ProductParam productParam = new ProductParam();
        JdippProductDetailParam param = new JdippProductDetailParam();
        param.setSku(Long.valueOf(productDetailParam.getProductId()));
        param.setLanguage(defaultLanguage);
        URI uri = constructUri(param, "jdipp.product.get_detail_bysku");
        JdippProductDetailResult result1 = RestUtil.post(uri, null, JdippProductDetailResult.class);
        if (result1 == null || result1.getModel() == null || result1.getModel().getData() == null) {
            return productParam;
        }
        JdippProductDetailResult.Sku sku = result1.getModel().getData();
        Map<String, Double> currencyMap = productDetailParam.getCurrencyMap();
        Double currencyRate = currencyMap.get(sku.getPriceVo().getCurrency());
        Map<String, Integer> langMap = productDetailParam.getLangMap();
        Integer platformLang = langMap.get(defaultLanguage);
        String language = productDetailParam.getLanguage();
        Integer specialLang = langMap.get(language);
        productParam.setPlatformName(productDetailParam.getPlatform().name());
        productParam.setPlatformProdId(productDetailParam.getProductId());
        productParam.setPlatformSyncTime(productDetailParam.getDateTime());

        productParam.setPlatformSoldNum(defaultSoldNum);
        productParam.setPlatformDetailJson(JSONUtil.toJsonStr(sku));
        productParam.setProdId(null);
        productParam.setUserId(null);
        productParam.setStatus(mapProdStatus(sku.getStatus()));
        productParam.setSkuStatus(productParam.getStatus() == 1 ? 1 : 0);
        productParam.setPreSellStatus(null);
        productParam.setPreSellTime(null);
        productParam.setProdName(sku.getSkuName());
//        productParam.setBrief(getBrief(detailModel));
        productParam.setContent(sku.getPcDescription());
        productParam.setProdType(6);
        productParam.setOriPrice(null);
        productParam.setMold(0);
        productParam.setNotMold(null);
        productParam.setProdIds(null);
        productParam.setSkuIds(null);
        productParam.setTotalStocks(sku.getStockNum());
        productParam.setPic(sku.getSkuImageUrl());
//        productParam.setVideo(detailModel.getDetailVideo());
        productParam.setImgs(String.join(",", sku.getImages()));
        productParam.setCategoryId(null);
        productParam.setShopCategoryId(null);
        productParam.setBrandId(null);
        productParam.setEmployeeId(null);
        productParam.setProdKeyType(1);
        productParam.setProdKey("0");
        productParam.setSupplierId(null);
//        boolean freePost = Arrays.stream(tagInfoList).anyMatch(t -> "isOnePsaleFreePost".equals(t.getKey()) && t.getValue() != null && t.getValue());
        productParam.setDeliveryTemplateId(/*freePost ? 0L : */-1L);
        productParam.setDeliveryAmount(/*freePost ? null : */10.0D); // todo,hxx动态运费
        productParam.setDeliveryTemplateIds(null);
        productParam.setIsDistribution(0);
        productParam.setIsTop(0);
        productParam.setUseLang(specialLang);
        productParam.setUseCountry(specialLang);
        return productParam;
    }

    @Override
    public String parseAddressCode(AddressCodeParseParam addressCodeParseParam) {
        return "";
    }

    @Override
    public PayUrlResult getPayUrl(PayUrlParam payUrlParam) {
        PayUrlResult payUrlResult = new PayUrlResult();
        JdippOrderPayParam param = new JdippOrderPayParam();
        if (payUrlParam.getOrderIds().length != 1) {
            throw new IllegalArgumentException("only one orderId is allowed");
        }
        param.setOrderId(Long.valueOf(payUrlParam.getOrderIds()[0]));
        URI uri = constructUri(param, "jdipp.order.pay");
        JdippCommonResult payResult = RestUtil.post(uri, param, JdippCommonResult.class);
        if (!payResult.getSuccess()) {
            payUrlResult.setErrorMessage(payResult.getMessage());
            return payUrlResult;
        }
        URI uri1 = constructUri(param, "jdipp.order.get_detail");
        JdippOrderDetailResult detail = RestUtil.post(uri1, param, JdippOrderDetailResult.class);
        payUrlResult.setPaySuccess(detail != null && detail.getModel() != null && detail.getModel().getData() != null
                && "READY_TO_SHIP".equals(detail.getModel().getData().getOrderStatus()));
        return payUrlResult;
    }

    @Override
    public CreateOrderResult createOrder(CreateOrderParam createOrderParam) {
        CreateOrderResult result = new CreateOrderResult();
        JdippOrderCreateParam param = new JdippOrderCreateParam();
        JdippOrderCreateParam.ItemInfo[] array = Arrays.stream(createOrderParam.getOrderItems()).map(orderItem -> {
            JdippOrderCreateParam.ItemInfo itemInfo = new JdippOrderCreateParam.ItemInfo();
            itemInfo.setNum(orderItem.getQuantity());
            itemInfo.setSkuId(Long.valueOf(orderItem.getProductId()));
            return itemInfo;
        }).toArray(JdippOrderCreateParam.ItemInfo[]::new);
        param.setItemInfos(array);
        param.setOpenOrderId(createOrderParam.getDateTime().toString());
        JdippOrderCreateParam.ConsigneeInfo consigneeInfo = new JdippOrderCreateParam.ConsigneeInfo();
        consigneeInfo.setCity(createOrderParam.getCityText());
        consigneeInfo.setMobile(createOrderParam.getMobile());
        consigneeInfo.setPhone(createOrderParam.getPhone());
        consigneeInfo.setFullname(createOrderParam.getFullName());
        consigneeInfo.setDistrict(createOrderParam.getDistrictCode());
        consigneeInfo.setAddress1(createOrderParam.getAddress());
//        address.setAreaText(createOrderParam.getAreaText());
//        address.setPostCode(createOrderParam.getPostCode());
//        address.setProvinceText(createOrderParam.getProvinceText());
//        address.setTownText(createOrderParam.getTownText());
        param.setConsigneeInfo(consigneeInfo);
        URI uri = constructUri(param, "jdipp.order.create");
//        JdippOrderCreateResult result1 = JSONUtil.toBean(RestUtil.post(uri, null, String.class), JdippOrderCreateResult.class);
        JdippOrderCreateResult result1 = RestUtil.post(uri, null, JdippOrderCreateResult.class);
        if (result1 == null || result1.getModel() == null || result1.getModel().getData() == null
                || result1.getModel().getData().getOrderId() == null) {
            if (result1 != null) {
                if (result1.getModel() != null && result1.getModel().getMessage() != null) {
                    result.setErrorMessage(result1.getModel().getMessage());
                } else {
                    result.setErrorMessage(result1.getMessage());
                }
            }
            return result;
        }
        result.setOrderNumber(result1.getModel().getData().getOrderId().toString());
        return result;
    }

    @Override
    public List<TrackingInfoResult> getTrackingInfo(OrderParam orderParam) {
        JdippOrderTrackingParam param = new JdippOrderTrackingParam();
        param.setOrderId(Long.valueOf(orderParam.getOrderId()));
        URI uri = constructUri(param, "jdipp.order.tracking");
        JdippOrderTrackingResult result = RestUtil.post(uri, null, JdippOrderTrackingResult.class);
        if (result == null || result.getModel() == null || result.getModel().getData() == null) {
            return new ArrayList<>();
        }
        return result.getModel().getData().stream().map(l -> {
            TrackingInfoResult trackingInfoResult = new TrackingInfoResult();
            trackingInfoResult.setLogisticId(l.getTrackingNo());
            if (l.getTraceDetail() != null) {
                List<TrackingInfoResult.Step> steps = l.getTraceDetail().stream().map(s -> {
                    TrackingInfoResult.Step step = new TrackingInfoResult.Step();
                    step.setTime(s.getEventTime());
                    step.setRemark(s.getEventDesc());
                    return step;
                }).collect(Collectors.toList());
                trackingInfoResult.setSteps(steps);
            }
            return trackingInfoResult;
        }).collect(Collectors.toList());
    }

    /**
     * 映射商品状态
     *
     * 平台商品状态（-1:删除、0:商家下架、1:上架、2:违规下架、3:平台审核）
     * jdipp商品状态（ 0:下架、1:上架）
     */
    private Integer mapProdStatus(Integer status) {
        switch (status) {
            case 1:
                return 1;
            default:
                return 0;
        }
    }

    private URI constructUri(Object param, String method) {
        String accessToken = stringRedisTemplate.opsForValue().get(PLATFORM_TOKEN_CACHE_PREFIX + PlatformEnum.JDIPP);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("request", param);
        String paramStr = JSONUtil.toJsonStr(hashMap);
        String timestamp = DateUtil.format(new Date(), dateFormat);
        try {
            return UriComponentsBuilder.fromHttpUrl(domain).encode(Charset.defaultCharset())
                    .queryParam("timestamp", timestamp.replace(" ", "%20").replace("+", "%2B"))
                    .queryParam("format", format)
                    .queryParam("method", method)
                    .queryParam("appKey", platformProperties.getJdippAppId())
                    .queryParam("accessToken", accessToken)
                    .queryParam("sign", generateSign(accessToken, method, paramStr, timestamp))
                    .queryParam("version", version)
                    .queryParam("param", URLEncoder.encode(paramStr, StandardCharsets.UTF_8.name()))
                    .build(true)
                    .toUri();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateSign(String accessToken, String method, String param, String timestamp) {
        String appId = platformProperties.getJdippAppId();
        String appSecret = platformProperties.getJdippAppSecret();
        String s = appSecret + "accessToken" + accessToken + "appKey" + appId + "format" + format + "method" + method
                + "param" + param + "timestamp" + timestamp + "version" + version + appSecret;
        return MD5.create().digestHex(s);
    }


    // --------

    private static URI constructUri1(Object param, String method) {
        // 正式
        // 正式获取令牌 https://api.joybuy.com/token?grantType=client_credentials&appKey=app_20250414000006132048&appSecret=dcaef67430f7ab420818d1c7adb20c93f9d1ff262
        String appId = "app_20250414000006132048";
        String appSecret = "dcaef67430f7ab420818d1c7adb20c93f9d1ff262";
        String accessToken = "588d6d869f399acd6f2824c9c62aa34aead7cb996";

        // 测试
        // 测试获取令牌 https://api.joybuy.com/token?grantType=client_credentials&appKey=app_20240402000006115028&appSecret=00b7a2212885aa898ce3611488d1c66e191d2d093
//        String appId = "app_20240402000006115028";
//        String appSecret = "00b7a2212885aa898ce3611488d1c66e191d2d093";
//        String accessToken = "ddb4d89261e9d88a9107482a805ffd46eba6b80c3";

        String dateFormat = "yyyy-MM-dd HH:mm:ss ZZZ";
        String format = "json";
        String version = "V1";
        String domain = "https://api.joybuy.com/router";
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("request", param);
        String paramStr = JSONUtil.toJsonStr(hashMap);
        String timestamp = DateUtil.format(new Date(), dateFormat);
        String s = appSecret + "accessToken" + accessToken + "appKey" + appId + "format" + format + "method" + method
                + "param" + paramStr + "timestamp" + timestamp + "version" + version + appSecret;
        String sign = MD5.create().digestHex(s);
        try {
            return UriComponentsBuilder.fromHttpUrl(domain).encode(Charset.defaultCharset())
                    .queryParam("timestamp", timestamp.replace(" ", "%20").replace("+", "%2B"))
                    .queryParam("format", format)
                    .queryParam("method", method)
                    .queryParam("appKey", appId)
                    .queryParam("accessToken", accessToken)
                    .queryParam("sign", sign)
                    .queryParam("version", version)
                    .queryParam("param", URLEncoder.encode(paramStr, StandardCharsets.UTF_8.name()))
                    .build(true)
                    .toUri();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {

        // SKU列表
        JdippProductListParam param = new JdippProductListParam();
        param.setPageSize(50);
        param.setLanguage("en");
        URI uri = constructUri1(param, "jdipp.product.list");
        System.out.println("SKU列表请求地址：" + uri);
        String post = RestUtil.post(uri, null, String.class);
        System.out.println("SKU列表响应参数：" + post);

        // 订单列表
        JdippOrderListParam param1 = new JdippOrderListParam();
        param1.setPageSize(10);
        param1.setCurrentPage(1);
        URI uri1 = constructUri1(param1, "jdipp.order.list");
        System.out.println("订单列表请求地址：" + uri1);
        String post1 = RestUtil.post(uri1, null, String.class);
        System.out.println("订单列表响应参数：" + post1);

        // 支付
        JdippOrderPayParam param2 = new JdippOrderPayParam();
        param2.setOrderId(311947639655L);
        URI uri2 = constructUri1(param1, "jdipp.order.pay");
        String post2 = RestUtil.post(uri2, null, String.class);
        System.out.println(post2);

        // 取消订单
        JdippOrderCancelParam param3 = new JdippOrderCancelParam();
        param3.setCancelReason("无理由");
        param3.setOrderId(316303210122L);
        URI uri3 = constructUri1(param2, "jdipp.order.cancel");
        JdippCommonResult post3 = RestUtil.post(uri3, null, JdippCommonResult.class);
        System.out.println(JSONUtil.toJsonStr(post3));
    }

}
