package com.example.demo.service;

import com.example.demo.entity.User;

public interface TestService {

    void testProxy();

    void testLock(Long id, User user);
}
