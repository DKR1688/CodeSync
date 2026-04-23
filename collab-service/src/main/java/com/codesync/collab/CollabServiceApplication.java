package com.codesync.collab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CollabServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CollabServiceApplication.class, args);
	}

}
