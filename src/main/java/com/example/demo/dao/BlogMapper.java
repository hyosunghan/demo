package com.example.demo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Blog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Created by hyosunghan on 2017/7/10.
 */
@Mapper
public interface BlogMapper extends BaseMapper<Blog> {

    List<Blog> testMapper();
}
