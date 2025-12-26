package com.example.demo.sdk.dto;

import lombok.Data;

@Data
public class ConfirmReceiveGoodsResult {

    private String orderId;

    private String errorMessage;
}
