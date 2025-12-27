package com.example.demo.service;

import com.example.demo.entity.Users;
import com.example.demo._lock.annotation.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TestServiceImpl implements TestService {

    @Override
    public void testProxy() {
        log.info("代理测试");
    }

    @Override
    @RedisLock(name = "LOCK:USERS", keys = {"#id", "#users.username"})
    public void testLock(Long id, Users users) {
        try {
            log.info("业务逻辑开始");
            Thread.sleep(7000);
            log.info("业务逻辑完成");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
