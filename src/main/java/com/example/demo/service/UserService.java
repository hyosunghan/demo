package com.example.demo.service;

import com.example.demo.entity.Users;

import java.util.Set;

public interface UserService {

    boolean checkUser(Users users);

    Users getUser(String username);

    Set<String> getUserPermissions(String username);

    Set<String> getUserRoles(String username);
}
