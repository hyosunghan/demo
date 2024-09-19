package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.Blog;

import java.util.List;

public interface BlogService extends IService<Blog> {

    List<Blog> testMapper();
}
