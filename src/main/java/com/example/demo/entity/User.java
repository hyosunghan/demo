package com.example.demo.entity;

import com.example.demo.annotation.CustomerInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@CustomerInfo
public class User implements Serializable {

    private Long id;

    private String username;

    @CustomerInfo
    private String password;
}
