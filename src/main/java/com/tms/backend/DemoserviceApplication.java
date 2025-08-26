package com.tms.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.tms") // widen the scan base
public class DemoserviceApplication {
	public static void main(String[] args) {
		SpringApplication.run(DemoserviceApplication.class, args);
	}
}
