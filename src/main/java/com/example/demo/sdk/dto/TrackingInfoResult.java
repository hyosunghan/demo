package com.example.demo.sdk.dto;

import lombok.Data;

import java.util.List;

@Data
public class TrackingInfoResult {

    private String logisticId;

    private String logisticBillNo;

    private List<Step> steps;

    private List<Node> nodes;

    @Data
    public static class Step {

        private String time;

        private String remark;
    }

    @Data
    public static class Node {

        private String action;

        private String areaCode;

        private String encrypt;

        private String acceptTime;

        private String remark;

        private String facilityType;

        private String facilityNo;

        private String facilityName;
    }
}
