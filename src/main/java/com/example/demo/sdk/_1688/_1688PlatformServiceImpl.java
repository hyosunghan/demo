package com.example.demo.sdk._1688;

import cn.hutool.json.JSONUtil;
import com.alibaba.fenxiao.crossborder.param.*;
import com.alibaba.logistics.param.AlibabaTradeGetLogisticsTraceInfoBuyerViewParam;
import com.alibaba.logistics.param.AlibabaTradeGetLogisticsTraceInfoBuyerViewResult;
import com.alibaba.ocean.rawsdk.ApiExecutor;
import com.alibaba.ocean.rawsdk.common.SDKResult;
import com.alibaba.trade.param.*;
import com.example.demo.sdk.AbstractPlatformServiceImpl;
import com.example.demo.sdk.PlatformProperties;
import com.example.demo.sdk.dto.*;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class _1688PlatformServiceImpl extends AbstractPlatformServiceImpl {

    private final PlatformProperties platformProperties;

    private final ApiExecutor apiExecutor;

    public _1688PlatformServiceImpl(PlatformProperties platformProperties) {
        this.platformProperties = platformProperties;
        this.apiExecutor = new ApiExecutor(platformProperties.get_1688AppId(), platformProperties.get_1688AppSecret());
    }

    @Override
    public TopProductsResult getTopProducts(TopProductsParam topProductsParam) {
        ProductTopListQueryParam productTopListQueryParam = new ProductTopListQueryParam();
        ProductTopListQueryRankQueryParams productTopListQueryRankQueryParams = new ProductTopListQueryRankQueryParams();
        productTopListQueryRankQueryParams.setLanguage(topProductsParam.getLanguage());
        productTopListQueryRankQueryParams.setLimit(topProductsParam.getLimit());
        productTopListQueryRankQueryParams.setRankType(topProductsParam.getRankType());
        productTopListQueryRankQueryParams.setRankId(topProductsParam.getCategoryId());
        productTopListQueryParam.setRankQueryParams(productTopListQueryRankQueryParams);
        SDKResult<ProductTopListQueryResult> result = apiExecutor.execute(productTopListQueryParam, platformProperties.get_1688AppToken());
        if (result == null || result.getResult() == null || result.getResult().getResult() == null
                || result.getResult().getResult().getResult() == null) {
            return null;
        }
        ProductTopListQueryRankModel rankModel = result.getResult().getResult().getResult();
        TopProductsResult listResult = new TopProductsResult();
        listResult.setRankName(rankModel.getRankName());
        ProductTopListQueryRankProductModel[] rankProductModels = rankModel.getRankProductModels();
        if (rankProductModels != null) {
            List<TopProductsResult.TopProductListResultItem> collect = Arrays.stream(rankProductModels).map(p -> {
                TopProductsResult.TopProductListResultItem item = new TopProductsResult.TopProductListResultItem();
                item.setItemId(p.getItemId().toString());
                item.setSort(p.getSort());
                item.setTitle(p.getTitle());
                item.setTranslateTitle(p.getTranslateTitle());
                item.setImgUrl(p.getImgUrl());
                item.setGoodsScore(p.getGoodsScore());
                item.setBuyerNum(p.getBuyerNum());
                item.setSoldOut(p.getSoldOut());
                return item;
            }).collect(Collectors.toList());
            listResult.setTopProductListResultItemList(collect);
        }
        return listResult;
    }

    @Override
    public ProductParam getProductDetail(ProductDetailParam productDetailParam) {
        Map<String, Double> currencyMap = productDetailParam.getCurrencyMap();
        Double currencyRate = currencyMap.get(platformProperties.get_1688DefaultCurrency());
        Map<String, Integer> langMap = productDetailParam.getLangMap();
        Integer platformLang = langMap.get(platformProperties.get_1688DefaultLang());
        String language = productDetailParam.getLanguage();
        Integer specialLang = langMap.get(language);
        ProductSearchQueryProductDetailParam productSearchQueryProductDetailParam = new ProductSearchQueryProductDetailParam();
        ProductSearchQueryProductDetailParamOfferDetailParam productSearchQueryProductDetailParamOfferDetailParam = new ProductSearchQueryProductDetailParamOfferDetailParam();
        productSearchQueryProductDetailParamOfferDetailParam.setCountry(productDetailParam.getLanguage());
        productSearchQueryProductDetailParamOfferDetailParam.setOfferId(Long.valueOf(productDetailParam.getProductId()));
        productSearchQueryProductDetailParam.setOfferDetailParam(productSearchQueryProductDetailParamOfferDetailParam);
        SDKResult<ProductSearchQueryProductDetailResult> result = apiExecutor.execute(productSearchQueryProductDetailParam, platformProperties.get_1688AppToken());
        if (result == null || result.getResult() == null || result.getResult().getResult() == null
                || result.getResult().getResult().getResult() == null) {
            if (JSONUtil.toJsonStr(result).contains("未开通对应语言")) {
                language = "en";
                productSearchQueryProductDetailParamOfferDetailParam.setCountry(language);
                specialLang = langMap.get(language);
                result = apiExecutor.execute(productSearchQueryProductDetailParam, platformProperties.get_1688AppToken());
                if (result == null || result.getResult() == null || result.getResult().getResult() == null
                        || result.getResult().getResult().getResult() == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        ProductSearchQueryProductDetailModelProductDetailModel detailModel = result.getResult().getResult().getResult();
        ProductParam productParam = new ProductParam();
        productParam.setPlatformName(productDetailParam.getPlatform().name());
        productParam.setPlatformProdId(detailModel.getOfferId().toString());
        productParam.setPlatformSyncTime(productDetailParam.getDateTime());
        ComAlibabaCbuOfferModelOutProductTagInfo[] tagInfoList = detailModel.getTagInfoList();
//        if (Arrays.stream(tagInfoList).noneMatch(t -> "isOnePsale".equals(t.getKey()) && t.getValue() != null && t.getValue())) {
//            return null;
//        }
        productParam.setPlatformSoldNum(Integer.parseInt(detailModel.getSoldOut()));
        productParam.setPlatformDetailJson(JSONUtil.toJsonStr(detailModel));
        productParam.setProdId(null);
        productParam.setUserId(null);
        productParam.setStatus(mapProdStatus(detailModel.getStatus()));
        productParam.setSkuStatus(productParam.getStatus() == 1 ? 1 : 0);
        productParam.setPreSellStatus(null);
        productParam.setPreSellTime(null);
        productParam.setProdName(detailModel.getSubjectTrans());
        productParam.setBrief(getBrief(detailModel));
        productParam.setContent(detailModel.getDescription());
        productParam.setProdType(6);
        productParam.setNotProdTypes(Arrays.asList(0, 1, 2, 3, 4, 5));
        productParam.setOriPrice(null);
        productParam.setMold(0);
        productParam.setNotMold(null);
        productParam.setNotMolds(Arrays.asList(1, 2, 3));
        productParam.setProdIds(null);
        productParam.setSkuIds(null);
        productParam.setTotalStocks(detailModel.getProductSaleInfo().getAmountOnSale());
        productParam.setPic(detailModel.getProductImage().getImages()[0]);
        productParam.setVideo(detailModel.getDetailVideo());
        productParam.setImgs(String.join(",", detailModel.getProductImage().getImages()));
        productParam.setCategoryId(null);
        productParam.setShopCategoryId(null);
        productParam.setBrandId(null);
        productParam.setEmployeeId(null);
        productParam.setProdKeyType(1);
        productParam.setProdKey("0");
        productParam.setSupplierId(null);
        boolean freePost = Arrays.stream(tagInfoList).anyMatch(t -> "isOnePsaleFreePost".equals(t.getKey()) && t.getValue() != null && t.getValue());
        productParam.setDeliveryTemplateId(freePost ? 0L : -1L);
        productParam.setDeliveryAmount(freePost ? null : 10.0D); // todo,hxx动态运费
        productParam.setDeliveryTemplateIds(null);
        productParam.setIsDistribution(0);
        productParam.setIsTop(0);
        productParam.setScorePrice(null);
        productParam.setSeq(null);
        productParam.setDeliveryMode(1);
        productParam.setVirtualRemark(null);
        productParam.setWriteOffNum(null);
        productParam.setWriteOffMultipleCount(null);
        productParam.setWriteOffTime(null);
        productParam.setWriteOffStart(null);
        productParam.setWriteOffEnd(null);
        productParam.setIsRefund(null);
        productParam.setSortParam(null);
        productParam.setSortType(null);
        productParam.setPartyCode(null);
        productParam.setEmployeeMobile(null);
        productParam.setUseLang(specialLang);
        productParam.setUseCountry(specialLang);
        productParam.setIsActive(null);
        productParam.setGroupActivityId(null);
        productParam.setIsStockWarning(null);
        productParam.setIsAllEntity(null);
        productParam.setIsBindVoucher(null);
        productParam.setDirections(null);
        return productParam;
    }

    @Override
    public PreviewOrderResult previewOrder(CreateOrderParam createOrderParam) {
        Map<String, Double> currencyMap = createOrderParam.getCurrencyMap();
        Double currencyRate = currencyMap.get(platformProperties.get_1688DefaultCurrency());
        AlibabaCreateOrderPreviewParam param = new AlibabaCreateOrderPreviewParam();
        param.setFlow("general");
        AlibabaTradeFastAddress address = new AlibabaTradeFastAddress();
        address.setAddress(createOrderParam.getAddress());
        address.setAreaText(createOrderParam.getAreaText());
        address.setMobile(createOrderParam.getMobile());
        address.setPhone(createOrderParam.getPhone());
        address.setDistrictCode(createOrderParam.getDistrictCode());
        address.setFullName(createOrderParam.getFullName());
        address.setCityText(createOrderParam.getCityText());
        address.setPostCode(createOrderParam.getPostCode());
        address.setProvinceText(createOrderParam.getProvinceText());
        address.setTownText(createOrderParam.getTownText());
        param.setAddressParam(address);
        AlibabaTradeFastCargo[] cargos = Arrays.stream(createOrderParam.getOrderItems()).map(item -> {
            AlibabaTradeFastCargo cargo = new AlibabaTradeFastCargo();
            cargo.setOfferId(Long.valueOf(item.getProductId()));
            cargo.setSpecId(item.getSpecId());
            cargo.setQuantity(Double.valueOf(item.getQuantity()));
            return cargo;
        }).toArray(AlibabaTradeFastCargo[]::new);
        param.setCargoParamList(cargos);
        SDKResult<AlibabaCreateOrderPreviewResult> result = apiExecutor.execute(param, platformProperties.get_1688AppToken());
        PreviewOrderResult previewOrderResult = new PreviewOrderResult();
        if (result == null || result.getResult() == null || result.getResult().getSuccess() == null || !result.getResult().getSuccess()) {
            previewOrderResult.setErrorMessage(getErrorMessage(result));
            return previewOrderResult;
        }
        AlibabaCreateOrderPreviewResultModel model = result.getResult().getOrderPreviewResuslt()[0];
        previewOrderResult.setProductAmount(calculateRounding(model.getSumPaymentNoCarriage(), currencyRate));
        previewOrderResult.setShippingAmount(calculateRounding(model.getSumCarriage(), currencyRate));
        previewOrderResult.setTotalAmount(calculateRounding(model.getSumPayment(), currencyRate));
        return previewOrderResult;
    }

    private double calculateRounding(Long fee, Double currencyRate) {
        BigDecimal consignPrice = new BigDecimal(fee)
                .divide(new BigDecimal(100), 2, RoundingMode.CEILING);
        return getCurrentPrice(consignPrice.toString(), currencyRate, null, null);
    }

    @Override
    public CreateOrderResult createOrder(CreateOrderParam createOrderParam) {
        Map<String, Double> currencyMap = createOrderParam.getCurrencyMap();
        Double currencyRate = currencyMap.get(platformProperties.get_1688DefaultCurrency());
        AlibabaTradeCreateCrossOrderParam param = new AlibabaTradeCreateCrossOrderParam();
        param.setFlow("general");
        AlibabaTradeFastAddress address = new AlibabaTradeFastAddress();
        address.setAddress(createOrderParam.getAddress());
        address.setAreaText(createOrderParam.getAreaText());
        address.setMobile(createOrderParam.getMobile());
        address.setPhone(createOrderParam.getPhone());
        address.setDistrictCode(createOrderParam.getDistrictCode());
        address.setFullName(createOrderParam.getFullName());
        address.setCityText(createOrderParam.getCityText());
        address.setPostCode(createOrderParam.getPostCode());
        address.setProvinceText(createOrderParam.getProvinceText());
        address.setTownText(createOrderParam.getTownText());
        param.setAddressParam(address);
        AlibabaTradeFastCargo[] cargos = Arrays.stream(createOrderParam.getOrderItems()).map(item -> {
            AlibabaTradeFastCargo cargo = new AlibabaTradeFastCargo();
            cargo.setOfferId(Long.valueOf(item.getProductId()));
            cargo.setSpecId(item.getSpecId());
            cargo.setQuantity(Double.valueOf(item.getQuantity()));
            return cargo;
        }).toArray(AlibabaTradeFastCargo[]::new);
        param.setCargoParamList(cargos);
        SDKResult<AlibabaTradeCreateCrossOrderResult> result = apiExecutor.execute(param, platformProperties.get_1688AppToken());
        CreateOrderResult createOrderResult = new CreateOrderResult();
        if (result == null || result.getResult() == null || result.getResult().getResult() == null
                || result.getResult().getResult().getSuccess() == null || !result.getResult().getResult().getSuccess()) {
            createOrderResult.setErrorMessage(getErrorMessage(result));
        } else {
            AlibabaTradeCrossResult result1 = result.getResult().getResult();
            createOrderResult.setOrderNumber(result1.getOrderId());
            createOrderResult.setShippingAmount(calculateRounding(result1.getPostFee(), currencyRate));
            createOrderResult.setTotalAmount(calculateRounding(result1.getTotalSuccessAmount(), currencyRate));
        }
        return createOrderResult;
    }

    private String getErrorMessage(SDKResult<?> result) {
        if (result == null) {
            return "internal error 1001";
        }
        if (result.getResult() == null) {
            if (result.getErrorMessage() != null
                    && !StringUtils.isEmpty(result.getErrorMessage())) {
                return result.getErrorMessage();
            }
            return "internal error 1002";
        }
        if (result.getResult().getClass().equals(AlibabaTradeCreateCrossOrderResult.class)) {
            AlibabaTradeCreateCrossOrderResult r = (AlibabaTradeCreateCrossOrderResult) result.getResult();
            if (r != null && r.getResult() != null && r.getResult().getMessage() != null && !StringUtils.isEmpty(r.getResult().getMessage())) {
                return r.getResult().getMessage();
            }
            if (r != null && r.getMessage() != null && !StringUtils.isEmpty(r.getMessage())) {
                return r.getMessage();
            }
        }
        if (result.getResult().getClass().equals(AlibabaCrossBorderPayUrlGetResult.class)) {
            AlibabaCrossBorderPayUrlGetResult r = (AlibabaCrossBorderPayUrlGetResult) result.getResult();
            if (r != null && r.getErrorMsg() != null && !StringUtils.isEmpty(r.getErrorMsg())) {
                return r.getErrorMsg();
            }
        }
        if (result.getResult().getClass().equals(TradeReceivegoodsConfirmResult.class)) {
            TradeReceivegoodsConfirmResult r = (TradeReceivegoodsConfirmResult) result.getResult();
            if (r != null && r.getResult() != null && r.getResult().getErrorInfo() != null && !StringUtils.isEmpty(r.getResult().getErrorInfo())) {
                return r.getResult().getErrorInfo();
            }
        }
        if (result.getResult().getClass().equals(AlibabaCreateOrderPreviewResult.class)) {
            AlibabaCreateOrderPreviewResult r =  (AlibabaCreateOrderPreviewResult) result.getResult();
            if (r != null && r.getErrorMsg() != null && !StringUtils.isEmpty(r.getErrorMsg())) {
                return r.getErrorMsg();
            }
        }
        return "internal error 1003";
    }


    @Override
    public String parseAddressCode(AddressCodeParseParam addressCodeParseParam) {
        AlibabaTradeAddresscodeParseParam param = new AlibabaTradeAddresscodeParseParam();
        param.setAddressInfo(addressCodeParseParam.getAddress());
        SDKResult<AlibabaTradeAddresscodeParseResult> result = apiExecutor.execute(param, platformProperties.get_1688AppToken());
        if (result == null || result.getResult() == null || result.getResult().getResult() == null) {
            return null;
        }
        return result.getResult().getResult().getAddressCode();
    }

    @Override
    public PayUrlResult getPayUrl(PayUrlParam payUrlParam) {
        AlibabaCrossBorderPayUrlGetParam param = new AlibabaCrossBorderPayUrlGetParam();
        param.setOrderIdList(Arrays.stream(payUrlParam.getOrderIds()).mapToLong(Long::valueOf).toArray());
        SDKResult<AlibabaCrossBorderPayUrlGetResult> result = apiExecutor.execute(param, platformProperties.get_1688AppToken());
        PayUrlResult payUrlResult = new PayUrlResult();
        if (result == null || result.getResult() == null || result.getResult().getPayUrl() == null || StringUtils.isEmpty(result.getResult().getPayUrl())) {
            payUrlResult.setErrorMessage(getErrorMessage(result));
        } else {
            payUrlResult.setPayUrl(result.getResult().getPayUrl());
            long[] cantPayOrderList = result.getResult().getCantPayOrderList();
            if (cantPayOrderList == null) {
                payUrlResult.setFailureOrderIds(new String[]{});
            } else {
                payUrlResult.setFailureOrderIds(Arrays.stream(cantPayOrderList).mapToObj(String::valueOf).toArray(String[]::new));
            }
        }
        return payUrlResult;
    }

    @Override
    public ConfirmReceiveGoodsResult confirmReceiveGoods(OrderParam orderParam) {
        TradeReceivegoodsConfirmParam param = new TradeReceivegoodsConfirmParam();
        param.setOrderId(Long.valueOf(orderParam.getOrderId()));
        param.setOrderEntryIds(new long[]{Long.valueOf(orderParam.getOrderId())});
        SDKResult<TradeReceivegoodsConfirmResult> result = apiExecutor.execute(param, platformProperties.get_1688AppToken());
        ConfirmReceiveGoodsResult confirmReceiveGoodsResult = new ConfirmReceiveGoodsResult();
        confirmReceiveGoodsResult.setOrderId(orderParam.getOrderId());
        if (result == null || result.getResult() == null || result.getResult().getResult() == null
                || result.getResult().getResult().getSuccess() == null || !result.getResult().getResult().getSuccess()) {
            confirmReceiveGoodsResult.setErrorMessage(getErrorMessage(result));
        }
        return confirmReceiveGoodsResult;
    }

    @Override
    public List<TrackingInfoResult> getTrackingInfo(OrderParam orderParam) {
        AlibabaTradeGetLogisticsTraceInfoBuyerViewParam param = new AlibabaTradeGetLogisticsTraceInfoBuyerViewParam();
        param.setOrderId(Long.valueOf(orderParam.getOrderId()));
        SDKResult<AlibabaTradeGetLogisticsTraceInfoBuyerViewResult> result = apiExecutor.execute(param, platformProperties.get_1688AppToken());
        if (result == null || result.getResult() == null || result.getResult().getSuccess() == null || !result.getResult().getSuccess()) {
            return new ArrayList<>();
        }
        if (result.getResult().getLogisticsTrace() == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(result.getResult().getLogisticsTrace()).map(l -> {
            TrackingInfoResult trackingInfoResult = new TrackingInfoResult();
            trackingInfoResult.setLogisticId(l.getLogisticsId());
            trackingInfoResult.setLogisticBillNo(l.getLogisticsBillNo());
            if (l.getLogisticsSteps() != null) {
                List<TrackingInfoResult.Step> steps = Arrays.stream(l.getLogisticsSteps()).map(s -> {
                    TrackingInfoResult.Step step = new TrackingInfoResult.Step();
                    step.setTime(s.getAcceptTime());
                    step.setRemark(s.getRemark());
                    return step;
                }).collect(Collectors.toList());
                trackingInfoResult.setSteps(steps);
            }
            if (l.getTraceNodeList() != null) {
                List<TrackingInfoResult.Node> nodes = Arrays.stream(l.getTraceNodeList())
                        .map(n -> JSONUtil.toBean(JSONUtil.toJsonStr(n), TrackingInfoResult.Node.class))
                        .collect(Collectors.toList());
                trackingInfoResult.setNodes(nodes);
            }
            return trackingInfoResult;
        }).collect(Collectors.toList());
    }

    private static String getBrief(ProductSearchQueryProductDetailModelProductDetailModel detailModel) {
        return detailModel.getSellingPoint() == null ? null : String.join(",", detailModel.getSellingPoint());
    }

    private static String getSkuProperties(ProductSearchQueryProductDetailModelSkuInfo sku, boolean usePlatformLang) {
        return Arrays.stream(sku.getSkuAttributes())
                .map(attr -> {
                    if (usePlatformLang) {
                        return attr.getAttributeName() + ":" + attr.getValue();
                    } else {
                        return attr.getAttributeNameTrans() + ":" + attr.getValueTrans();
                    }
                }).sorted().collect(Collectors.joining(";"));
    }

    /**
     * 映射商品状态
     *
     * 平台商品状态（-1:删除、0:商家下架、1:上架、2:违规下架、3:平台审核）
     * 1688商品状态（ published:上网状态;                 1
     *              member expired:会员撤销;            0
     *              auto expired:自然过期;              0
     *              expired:过期(包含手动过期与自动过期);  0
     *              member deleted:会员删除;            -1
     *              modified:修改;                    1
     *              new:新发;                         1
     *              deleted:删除;                     -1
     *              TBD:to be delete;                 -1
     *              approved:审批通过;                  1
     *              auditing:审核中;                   3
     *              untread:审核不通过;）                 2
     */
    private Integer mapProdStatus(String status) {
        switch (status) {
            case "new":
            case "update":
            case "published":
            case "approved":
                return 1;
            case "member deleted":
            case "deleted":
            case "TBD":
                return -1;
            case "untread":
                return 2;
            case "auditing":
                return 3;
            default:
                return 0;
        }
    }

    public static void main(String[] args) {
        String token = "783c2ba3-906d-478c-9ae4-7c54c8177e1d";
//        String userId = "23423532fwef";
        String appKey = "4278057";
        String appSecret = "dgNm1lU4EW";
//        String productCollectionId = "702309024731";
        ApiExecutor apiExecutor = new ApiExecutor(appKey, appSecret);

        // 热销商品
//        topProducts(apiExecutor, token);

        // 商品详情
        productDetail(apiExecutor, token);

        // 预览订单
//        previewOrder(apiExecutor, token);

        // 创建订单
//        createOrder(apiExecutor, token);

        // 取消交易
//        cancelTrade(apiExecutor, token);

        // 订单列表
//        orderList(apiExecutor, token);

        // 解析地区码
//        parseAddress(apiExecutor, token);

        // 查看订单支持的支付方式
//        queryPayWay(apiExecutor, token);

        // 跨境宝链接
//        crossBorderPayUrl(apiExecutor, token);

        // 免密支付
//        preparePay(apiExecutor, token);

    }

    private static void preparePay(ApiExecutor apiExecutor, String token) {
        AlibabaTradePayProtocolPayPreparePayParam param = new AlibabaTradePayProtocolPayPreparePayParam();
        AlibabaOceanOpenplatformBizTradeParamTradeWithholdPreparePayParam p = new AlibabaOceanOpenplatformBizTradeParamTradeWithholdPreparePayParam();
        p.setOrderId(4169757385897317637L);
        param.setTradeWithholdPreparePayParam(p);
        SDKResult<AlibabaTradePayProtocolPayPreparePayResult> execute = apiExecutor.execute(param, token);
        System.out.println(JSONUtil.toJsonStr(execute));
    }

    private static void crossBorderPayUrl(ApiExecutor apiExecutor, String token) {
        AlibabaCrossBorderPayUrlGetParam param = new AlibabaCrossBorderPayUrlGetParam();
        param.setOrderIdList(new long[]{4169757385897317637L});
        SDKResult<AlibabaCrossBorderPayUrlGetResult> execute = apiExecutor.execute(param, token);
        System.out.println(JSONUtil.toJsonStr(execute));
    }

    private static void queryPayWay(ApiExecutor apiExecutor, String token) {
        AlibabaTradePayWayQueryParam param = new AlibabaTradePayWayQueryParam();
        param.setOrderId("4169757385897317637");
        SDKResult<AlibabaTradePayWayQueryResult> execute = apiExecutor.execute(param, token);
        System.out.println(JSONUtil.toJsonStr(execute));
    }

    private static void parseAddress(ApiExecutor apiExecutor, String token) {
        AlibabaTradeAddresscodeParseParam param = new AlibabaTradeAddresscodeParseParam();
        param.setAddressInfo("China Shaanxi Xi'an 雁塔区");
        SDKResult<AlibabaTradeAddresscodeParseResult> result = apiExecutor.execute(param, token);
        System.out.println(JSONUtil.toJsonStr(result));
    }

    private static void previewOrder(ApiExecutor apiExecutor, String token) {
        AlibabaCreateOrderPreviewParam param = new AlibabaCreateOrderPreviewParam();
        param.setFlow("general");
        AlibabaTradeFastAddress address = new AlibabaTradeFastAddress();
        address.setAddress("大寨路12号");
//        address.setAddressId(1L); //
        address.setAreaText("未央区");
        address.setMobile("13145647897");
        address.setPhone("02901832712");
        address.setDistrictCode("610112");
        address.setFullName("张三");
        address.setCityText("西安");
        address.setPostCode("710016");
        address.setProvinceText("陕西省");
        address.setTownText("黄良镇");
        param.setAddressParam(address);
        AlibabaTradeFastCargo[] cargos = new AlibabaTradeFastCargo[1];
        AlibabaTradeFastCargo cargo = new AlibabaTradeFastCargo();
        cargo.setOfferId(776572878651L); // 商品ID
        cargo.setSpecId("9ddcbdb6d1812d962e31300a62ccded9");
//        cargo.setOpenOfferId("asdsad");       // 加密商品ID
        cargo.setQuantity(1D);  // 数量
//        cargo.setOutMemberId();    // 下游会员ID
        cargos[0] = cargo;
        param.setCargoParamList(cargos);
        SDKResult<AlibabaCreateOrderPreviewResult> execute = apiExecutor.execute(param, token);
        System.out.println(JSONUtil.toJsonStr(execute));
    }

    private static void topProducts(ApiExecutor apiExecutor, String token) {
        ProductTopListQueryParam productTopListQueryParam = new ProductTopListQueryParam();
        ProductTopListQueryRankQueryParams productTopListQueryRankQueryParams = new ProductTopListQueryRankQueryParams();
        productTopListQueryRankQueryParams.setLanguage("ja");
        productTopListQueryRankQueryParams.setLimit(20);
        productTopListQueryRankQueryParams.setRankType("hot");
        productTopListQueryRankQueryParams.setRankId("1");
        productTopListQueryParam.setRankQueryParams(productTopListQueryRankQueryParams);
        SDKResult<ProductTopListQueryResult> execute1 = apiExecutor.execute(productTopListQueryParam, token);
        System.out.println("热销商品" + JSONUtil.toJsonStr(execute1));
    }

    private static void productDetail(ApiExecutor apiExecutor, String token) {
        ProductSearchQueryProductDetailParam productSearchQueryProductDetailParam = new ProductSearchQueryProductDetailParam();
        ProductSearchQueryProductDetailParamOfferDetailParam productSearchQueryProductDetailParamOfferDetailParam = new ProductSearchQueryProductDetailParamOfferDetailParam();
        productSearchQueryProductDetailParamOfferDetailParam.setCountry("en");
        productSearchQueryProductDetailParamOfferDetailParam.setOfferId(740734839357L);
//        productSearchQueryProductDetailParamOfferDetailParam.setOutMemberId("23423532fwef");
        productSearchQueryProductDetailParam.setOfferDetailParam(productSearchQueryProductDetailParamOfferDetailParam);
        SDKResult<ProductSearchQueryProductDetailResult> execute = apiExecutor.execute(productSearchQueryProductDetailParam, token);
        System.out.println("商品详情" + JSONUtil.toJsonStr(execute));
    }

    private static void cancelTrade(ApiExecutor apiExecutor, String token) {
        AlibabaTradeCancelParam alibabaTradeCancelParam = new AlibabaTradeCancelParam();
        alibabaTradeCancelParam.setWebSite("1688");
        alibabaTradeCancelParam.setTradeID(4158077356724317637L);
        alibabaTradeCancelParam.setCancelReason("buyerCancel");
        SDKResult<AlibabaTradeCancelResult> execute = apiExecutor.execute(alibabaTradeCancelParam, token);
        System.out.println("取消订单" + JSONUtil.toJsonStr(execute));
    }

    private static void orderList(ApiExecutor apiExecutor, String token) {
        AlibabaTradeGetBuyerOrderListParam alibabaTradeGetBuyerOrderListParam = new AlibabaTradeGetBuyerOrderListParam();
        SDKResult<AlibabaTradeGetBuyerOrderListResult> execute = apiExecutor.execute(alibabaTradeGetBuyerOrderListParam, token);
        System.out.println("订单列表" + JSONUtil.toJsonStr(execute));
        if (execute == null || execute.getResult() == null || execute.getResult().getResult() == null) {
            return;
        }
        AlibabaOpenplatformTradeModelTradeInfo[] result = execute.getResult().getResult();
        Map<String, List<AlibabaOpenplatformTradeModelTradeInfo>> collect = Arrays.stream(result).collect(Collectors.groupingBy(a -> a.getBaseInfo().getStatus()));
        System.out.println("订单列表按状态分组" + JSONUtil.toJsonStr(collect));
        collect.forEach((status, order) -> {
            AlibabaOpenplatformTradeModelTradeInfo alibabaOpenplatformTradeModelTradeInfo = order.get(0);
            Long id = alibabaOpenplatformTradeModelTradeInfo.getBaseInfo().getId();
            AlibabaTradeGetBuyerViewParam alibabaTradeGetBuyerViewParam = new AlibabaTradeGetBuyerViewParam();
            alibabaTradeGetBuyerViewParam.setOrderId(id);
            alibabaTradeGetBuyerViewParam.setWebSite("1688");
            SDKResult<AlibabaTradeGetBuyerViewResult> execute1 = apiExecutor.execute(alibabaTradeGetBuyerViewParam, token);
            System.out.println("订单状态" + status + "订单详情" + JSONUtil.toJsonStr(execute1));
        });
    }

    private static void createOrder(ApiExecutor apiExecutor, String token) {
        AlibabaTradeCreateCrossOrderParam param = new AlibabaTradeCreateCrossOrderParam();
        param.setFlow("fenxiao");
        AlibabaTradeFastAddress address = new AlibabaTradeFastAddress();
        address.setAddress("大寨路12号");
//        address.setAddressId(1L); //
        address.setAreaText("未央区");
        address.setMobile("13145647897");
        address.setPhone("02901832712");
        address.setDistrictCode("610112");
        address.setFullName("张三");
        address.setCityText("西安");
        address.setPostCode("710016");
        address.setProvinceText("陕西省");
        address.setTownText("黄良镇");
        param.setAddressParam(address);
        AlibabaTradeFastCargo[] cargos = new AlibabaTradeFastCargo[1];
        AlibabaTradeFastCargo cargo = new AlibabaTradeFastCargo();
        cargo.setOfferId(776572878651L); // 商品ID
        cargo.setSpecId("9ddcbdb6d1812d962e31300a62ccded9");
//        cargo.setOpenOfferId("asdsad");       // 加密商品ID
        cargo.setQuantity(1D);  // 数量
//        cargo.setOutMemberId();    // 下游会员ID
        cargos[0] = cargo;
        param.setCargoParamList(cargos);
        SDKResult<AlibabaTradeCreateCrossOrderResult> execute = apiExecutor.execute(param, token);
        System.out.println(JSONUtil.toJsonStr(execute));
    }
}
