package com.example.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.demo.mapper")
@SpringBootApplication
public class DemoServiceApplication {

	public static void main(String[] args) {
		System.setProperty("JM.LOG.PATH", "logs/nacos");
		System.setProperty("JM.SNAPSHOT.PATH", "logs/nacos");
		SpringApplication.run(DemoServiceApplication.class, args);
	}
}
