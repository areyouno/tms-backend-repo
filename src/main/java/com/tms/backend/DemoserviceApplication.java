package com.tms.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan("com.tms")
@EnableAsync
public class DemoserviceApplication {
	public static void main(String[] args) {
		SpringApplication.run(DemoserviceApplication.class, args);
	}
}
