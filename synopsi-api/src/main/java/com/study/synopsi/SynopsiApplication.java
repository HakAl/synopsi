package com.study.synopsi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SynopsiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SynopsiApplication.class, args);
	}

}
