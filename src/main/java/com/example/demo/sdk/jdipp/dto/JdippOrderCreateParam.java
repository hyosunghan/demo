package com.example.demo.sdk.jdipp.dto;

import lombok.Data;

@Data
public class JdippOrderCreateParam {

    private String openOrderId;

    private ItemInfo[] itemInfos;

    private ConsigneeInfo consigneeInfo;

    @Data
    public static class ItemInfo {

        private Long skuId;

        private Integer num;
    }

    @Data
    public static class ConsigneeInfo {

        private String countryCode;

        private Long jdCountryId;

        private Long jdStateId;

        private String city;

        private String district;

        private String fullname;

        private String email;

        private String phone;

        private String mobile;

        private String address1;

        private String address2;
    }

}
