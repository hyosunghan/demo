package com.example.demo.service;

import com.example.demo.entity.Users;

public interface TestService {

    void testProxy();

    void testLock(Long id, Users users);
}
