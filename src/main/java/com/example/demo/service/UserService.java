package com.example.demo.service;

import com.example.demo.entity.Users;

import java.util.List;
import java.util.Set;

public interface UserService {

    List<Users> testMapper();

    boolean checkUser(Users users);

    Users getUser(String username);

    Set<String> getUserPermissions(String username);

    Set<String> getUserRoles(String username);
}
