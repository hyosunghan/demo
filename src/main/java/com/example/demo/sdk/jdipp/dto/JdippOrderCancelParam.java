package com.example.demo.sdk.jdipp.dto;

import lombok.Data;

@Data
public class JdippOrderCancelParam {

    private Long orderId;

    private String cancelReason;
}
