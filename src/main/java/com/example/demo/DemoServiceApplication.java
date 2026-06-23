package com.example.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.demo.mapper")
@SpringBootApplication
public class DemoServiceApplication {

	public static void main(String[] args) {
		// 设置nacos日志及缓存路径
		System.setProperty("JM.LOG.PATH", "logs");
		System.setProperty("JM.SNAPSHOT.PATH", "logs");
		// 启动服务
		SpringApplication.run(DemoServiceApplication.class, args);
	}
}
