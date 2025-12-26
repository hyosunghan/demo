package com.example.demo.sdk.dto;

import lombok.Data;

@Data
public class CreateOrderParam extends BaseParam {

    private OrderItem[] orderItems;

    private String address;

    private String areaText;

    private String mobile;

    private String phone;

    private String districtCode;

    private String fullName;

    private String cityText;

    private String postCode;

    private String provinceText;

    private String townText;

    @Data
    public static class OrderItem {

        private String specId;

        private Integer quantity;

        private String productId;
    }
}
