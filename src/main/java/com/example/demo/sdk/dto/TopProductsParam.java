package com.example.demo.sdk.dto;

import lombok.Data;

@Data
public class TopProductsParam extends BaseParam {

    /**
     * 分类id
     */
    private String categoryId;

    /**
     * 获取数量
     */
    private Integer limit;

    /**
     * 排序方式
     */
    private String rankType;

    /**
     * 游标
     */
    private String cursor;
}
