package com.example.demo.sdk.jdipp.dto;

import lombok.Data;

@Data
public class JdippOrderCreateResult {

    private Model model;

    private String message;

    @Data
    public static class Model {

        private Result data;

        private String message;
    }

    @Data
    public static class Result {

        private String openOrderId;

        private Long orderId;
    }
}
