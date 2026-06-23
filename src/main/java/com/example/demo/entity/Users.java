package com.example.demo.entity;

import com.example.demo.annotation.CustomerInfo;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@CustomerInfo
public class Users implements Serializable {

    private Long id;

    private String username;

    private String password;

    @CustomerInfo
    private String phoneNumber;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date birthday;

    private Integer status;
}
