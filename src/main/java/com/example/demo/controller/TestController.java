package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import com.example.demo.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Object test() {
        List<User> list = userService.testMapper();
        return list;
    }

    @GetMapping("/testWait")
    public Object testWait() {
        User user = new User();
        user.setUsername("w");
        new Thread(()-> testService.testWaitLock(1L, user)).start();
        new Thread(()-> testService.testWaitLock(1L, user)).start();
        return 2;
    }

    @GetMapping("/testExec")
    public Object testExec() {
        User user = new User();
        user.setUsername("e");
        new Thread(() -> testService.testExecLock(2L, user)).start();
        new Thread(() -> testService.testExecLock(2L, user)).start();
        return 3;
    }
}
