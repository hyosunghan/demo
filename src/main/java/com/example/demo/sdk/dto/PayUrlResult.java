package com.example.demo.sdk.dto;

import lombok.Data;

@Data
public class PayUrlResult {

    private Boolean paySuccess;

    private String payUrl;

    private String[] failureOrderIds;

    private String errorMessage;
}
