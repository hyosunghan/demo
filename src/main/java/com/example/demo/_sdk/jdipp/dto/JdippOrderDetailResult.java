package com.example.demo._sdk.jdipp.dto;

import lombok.Data;

@Data
public class JdippOrderDetailResult {

    private Model model;

    @Data
    public static class Model {

        private Data1 data;

    }

    @Data
    public static class Data1 {

        private String orderStatus;
    }

}
