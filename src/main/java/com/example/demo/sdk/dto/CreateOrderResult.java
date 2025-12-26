package com.example.demo.sdk.dto;

import lombok.Data;

@Data
public class CreateOrderResult {

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
     * 订单编号
     */
    private String orderNumber;

    /**
     * 错误信息
     */
    private String errorMessage;
}
