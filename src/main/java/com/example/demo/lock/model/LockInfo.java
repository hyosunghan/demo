package com.example.demo.lock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LockInfo {

    private String lockName;

    private String lockValue;

    private Integer waitTime;

    private Integer leaseTime;

    private Integer frequency;

}
