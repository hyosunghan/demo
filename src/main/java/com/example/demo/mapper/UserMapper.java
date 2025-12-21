package com.example.demo.mapper;

import com.example.demo.entity.Users;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    Users findByUsername(String username);

    List<Users> testMapper();
}
