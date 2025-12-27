package com.example.demo._sdk.jdipp.dto;

import lombok.Data;

@Data
public class JdippProductDetailResult {

    private Model model;

    @Data
    public static class Model {

        private String nextCursor;

        private Long total;

        private Sku data;
    }

    @Data
    public static class Sku {

        private String skuName;

        private PriceVo priceVo;

        private Integer status;

        private String pcDescription;

        private Integer stockNum;

        private String skuImageUrl;

        private String[] images;

        private AttrVos[] extAttrVos;

        private AttrGroups[] specAttrVos;

        private Double weight;
    }

    @Data
    public static class PriceVo {

        private Double price;

        private String currency;
    }

    @Data
    public static class AttrGroups {

        private Long attrGroupId;

        private String attrGroupName;

        private AttrVos[] attributeVos;

        private Integer order;
    }

    @Data
    public static class AttrVos {

        private Long attrId;

        private String attrName;

        private AttrValue[] attributeValueVos;
    }

    @Data
    public static class AttrValue {

        private Long attrValueId;

        private String attrValueName;

        private Integer order;
    }
}
