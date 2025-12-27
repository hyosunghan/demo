package com.example.demo._sdk.dto;

import lombok.Data;

import java.util.List;

@Data
public class TopProductsResult {

    private String rankName;

    private String cursor;

    private Long total;

    private List<TopProductListResultItem> topProductListResultItemList;

    @Data
    public static class TopProductListResultItem {
        private String itemId;
        private String title;
        private String translateTitle;
        private String imgUrl;
        private Integer sort;
        private Integer buyerNum;
        private Integer soldOut;
        private String goodsScore;
        private Double price;
    }
}
