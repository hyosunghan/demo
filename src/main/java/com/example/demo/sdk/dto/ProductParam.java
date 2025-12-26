package com.example.demo.sdk.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 商品参数
 *
 * @author LGH
 */
@Data
public class ProductParam {

    private Long prodId;

    private Long shopId;

    private String userId;

    private Integer status;

    private Integer skuStatus;

    private Integer preSellStatus;

    private Date preSellTime;

    private String prodName;

    private String brief;

    private String content;

    private Integer prodType;

    private List<Integer> notProdTypes;

    private Double price;

    private Double oriPrice;

    private Integer mold;

    private Integer notMold;

    private List<Integer> notMolds;

    private List<Long> prodIds;

    private List<Long> skuIds;

    private Integer totalStocks;

    private String pic;

    private String video;

    private String imgs;

    private Long categoryId;

    private Long shopCategoryId;

    private Long brandId;

    private Long employeeId;

    private Integer prodKeyType;

    private String prodKey;

    private Long supplierId;

    private String shopName;

    private Integer lang;

    private Long deliveryTemplateId;

    private Double deliveryAmount;
    private String deliveryTemplateIds;

    private Integer isDistribution;

    private Integer isTop;

    private Integer scorePrice;

    private Integer seq;

    private Integer deliveryMode;

    private String virtualRemark;

    private String writeOffNum;

    private Integer writeOffMultipleCount;

    private Integer writeOffTime;

    private Date writeOffStart;

    private Date writeOffEnd;

    private Integer isRefund;

    private Integer sortParam;

    private Integer sortType;

    private String partyCode;

    private String employeeMobile;

    private Integer useLang;
    private Integer useCountry;

    private Integer defaultLang;

    private Integer isActive;

    private Long groupActivityId;

    private Integer isStockWarning;

    private Integer isAllEntity;

    private Integer isBindVoucher;

    private String directions;

    private String platformName;

    private String platformProdId;

    private Long platformSyncTime;

    private String platformDetailJson;

    private Integer platformSoldNum;
}
