package com.brieflyai.BrieflyAI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class BrieflyAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BrieflyAiApplication.class, args);
	}

}
