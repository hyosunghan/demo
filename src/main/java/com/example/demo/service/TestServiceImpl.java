package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.lock.annotation.RedisLock;
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
    @RedisLock(name = "LOCK:USER", keys = {"#id", "#user.username"})
    public void testLock(Long id, User user) {
        try {
            log.info("业务逻辑开始");
            Thread.sleep(7000);
            log.info("业务逻辑完成");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
