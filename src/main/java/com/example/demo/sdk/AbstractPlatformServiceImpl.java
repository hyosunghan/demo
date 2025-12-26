package com.example.demo.sdk;

import com.example.demo.sdk.dto.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public abstract class AbstractPlatformServiceImpl implements IPlatformService {

    /**
     * 转换汇率
     *
     * @param consignPrice
     * @param currencyRate
     * @param fixProfitString
     * @param rateProfitString
     * @return
     */
    protected Double getCurrentPrice(String consignPrice, Double currencyRate, String fixProfitString, String rateProfitString) {
        if (consignPrice == null) {
            return null;
        }
        BigDecimal price = new BigDecimal(consignPrice)
                .divide(new BigDecimal(currencyRate), 2, RoundingMode.CEILING);
        if (fixProfitString == null || rateProfitString == null) {
            return price.doubleValue();
        }
        return price.add(getProfit(price, fixProfitString, rateProfitString)).doubleValue();
    }

    private BigDecimal getProfit(BigDecimal price, String fixProfitString, String rateProfitString) {
        BigDecimal fixProfit = new BigDecimal(fixProfitString);
        BigDecimal rate = new BigDecimal(rateProfitString);
        BigDecimal rateProfit = price.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return fixProfit.compareTo(rateProfit) > 0 ? fixProfit : rateProfit;
    }

    @Override
    public RefreshTokenParam refreshToken(RefreshTokenParam refreshTokenParam) {
        return null;
    }

    @Override
    public TopProductsResult getTopProducts(TopProductsParam topProductsParam) {
        return null;
    }

    @Override
    public ProductParam getProductDetail(ProductDetailParam productDetailParam) {
        return null;
    }

    @Override
    public PreviewOrderResult previewOrder(CreateOrderParam createOrderParam) {
        return new PreviewOrderResult();
    }

    @Override
    public CreateOrderResult createOrder(CreateOrderParam createOrderParam) {
        return new CreateOrderResult();
    }

    @Override
    public String parseAddressCode(AddressCodeParseParam addressCodeParseParam) {
        return null;
    }

    @Override
    public PayUrlResult getPayUrl(PayUrlParam payUrlParam) {
        return null;
    }

    @Override
    public List<TrackingInfoResult> getTrackingInfo(OrderParam orderParam) {
        return null;
    }

    @Override
    public ConfirmReceiveGoodsResult confirmReceiveGoods(OrderParam orderParam) {
        return new ConfirmReceiveGoodsResult();
    }
}
