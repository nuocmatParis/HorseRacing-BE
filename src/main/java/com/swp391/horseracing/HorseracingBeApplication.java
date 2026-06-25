package com.swp391.horseracing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HorseracingBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(HorseracingBeApplication.class, args);
	}

}
