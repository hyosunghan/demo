package com.example.demo.sdk.dto;

import lombok.Data;

@Data
public class PreviewOrderResult {

    /**
     * 产品金额
     */
    private Double productAmount;

    /**
     * 运费
     */
    private Double shippingAmount;

    /**
     * 总金额
     */
    private Double totalAmount;

    /**
     * 错误信息
     */
    private String errorMessage;
}
