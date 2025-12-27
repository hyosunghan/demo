package com.example.demo._sdk.jdipp.dto;

import lombok.Data;

import java.util.List;

@Data
public class JdippOrderTrackingResult {

    private Model model;

    @Data
    public static class Model {

        private List<Result> data;
    }

    @Data
    public static class Result {

        private Long orderId;

        private String trackingNo;

        private List<Detail> traceDetail;
    }

    @Data
    public static class Detail {

        private String eventDesc;

        private String eventTime;
    }

}
