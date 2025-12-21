package com.example.demo.controller;

import com.example.demo.entity.Users;
import com.example.demo.service.UserService;
import com.example.demo.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private TestService testService;

    @Autowired
    private UserService userService;

    @GetMapping("/test")
    @PreAuthorize("hasAuthority('user')")
    public Object test() {
        List<Users> list = userService.testMapper();
        return list;
    }

    @GetMapping("/testLock")
    @PreAuthorize("hasAuthority('admin')")
    public Object testLock() {
        Users users = new Users();
        users.setUsername("张三");
        new Thread(()-> testService.testLock(1L, users)).start();
        new Thread(()-> testService.testLock(1L, users)).start();
        return 2;
    }
}
