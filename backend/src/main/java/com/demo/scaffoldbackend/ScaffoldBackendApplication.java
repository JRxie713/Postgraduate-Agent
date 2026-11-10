package com.demo.scaffoldbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScaffoldBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScaffoldBackendApplication.class, args);
    }

}
