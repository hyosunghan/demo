package com.example.demo.controller;

import com.example.demo.auth.annotation.RequirePermission;
import com.example.demo.dto.Result;
import com.example.demo.entity.Users;
import com.example.demo.service.UserService;
import com.example.demo.service.TestService;
import com.example.demo.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping("/test")
@RefreshScope
public class TestController {

    @Autowired
    private TestService testService;

    @Autowired
    private UserService userService;

    @Value("${test.message:null}")
    private String message;

    @PostMapping("/login")
    public Result login(@RequestBody Users dto) {
        // 1. 验证用户名密码
        boolean result = userService.checkUser(dto);
        if (!result) {
            return Result.failure(400,"用户名或密码错误");
        }

        // 2. 生成JWT
        String token = JwtUtil.generateToken(
                dto.getUsername(),
                userService.getUserRoles(dto.getUsername()),
                userService.getUserPermissions(dto.getUsername()));

        HashMap<String, String> map = new HashMap<>();
        map.put("token", token);
        map.put("expire", String.valueOf(JwtUtil.EXPIRATION));
        return Result.success(map);
    }

    @GetMapping("/nacos")
    @RequirePermission("user:user")
    public Object test() {
        return Result.success(message);
    }

    @GetMapping("/lock")
    @RequirePermission("user:add")
    public Object testLock() {
        Users users = new Users();
        users.setUsername("张三");
        new Thread(()-> testService.testLock(1L, users)).start();
        new Thread(()-> testService.testLock(1L, users)).start();
        return 2;
    }
}
