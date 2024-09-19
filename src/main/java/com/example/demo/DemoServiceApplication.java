package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;

//@SpringBootApplication
public class DemoServiceApplication {

	public static void main(String[] args) {
//		SpringApplication.run(DemoServiceApplication.class, args);


	}

	public static String[] reorderLogFiles(String[] logs) {
		List<String> z = new ArrayList<>();
		List<String> s = new ArrayList<>();

		for (int i=0;i< logs.length; i++) {
			char c = logs[i].charAt(logs[i].length() - 1);
			boolean isNum = Character.isDigit(c);
			if (isNum) {
				s.add(logs[i]);
			} else {
				z.add(logs[i]);
			}
		}
		z.sort((a, b) -> {
			String[] s1 = a.split(" ", 2);
			String[] s2 = b.split(" ", 2);
			int i = s1[1].compareTo(s2[1]);
			return i == 0 ? s1[0].compareTo(s2[0]) : i;
		});
		z.addAll(s);
		return z.toArray(new String[0]);
	}
}
