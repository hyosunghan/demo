package com.example.demo._sdk.jdipp.dto;

import lombok.Data;

import java.util.List;

@Data
public class JdippProductListResult {

    private Model model;

    @Data
    public static class Model {

        private String nextCursor;

        private Long total;

        private List<Sku> data;
    }

    @Data
    public static class Sku {

        private String skuImageUrl;

        private String sku;

        private Long createTime;
    }
}
