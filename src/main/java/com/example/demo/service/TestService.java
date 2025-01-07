package com.example.demo.service;

import com.example.demo.entity.User;

public interface TestService {

    void testProxy();

    void testWaitLock(Long id, User user);

    void testExecLock(Long id, User user);
}
