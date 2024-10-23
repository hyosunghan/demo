package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.IntStream;

@Slf4j
@SpringBootApplication
public class DemoServiceApplication implements ApplicationRunner {

	public static void main(String[] args) {
		SpringApplication.run(DemoServiceApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
//		ForkJoinPool forkJoinPool = new ForkJoinPool(10);
//		ForkJoinTask<Integer> joinTask = (ForkJoinTask<Integer>) forkJoinPool.submit(() -> {
//			return IntStream.range(0, 100).parallel().map(i -> {
//                log.info("current number: {}", i);
//				return i;
//			}).sum();
//		});
//		log.info("sum: {}", joinTask.get());
	}
}
