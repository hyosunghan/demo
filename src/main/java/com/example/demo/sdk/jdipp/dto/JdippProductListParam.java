package com.example.demo.sdk.jdipp.dto;

import lombok.Data;

@Data
public class JdippProductListParam {

    private Integer pageSize;

    private String language;

    private String nextCursor;
}
