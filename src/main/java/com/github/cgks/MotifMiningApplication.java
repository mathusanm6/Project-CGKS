package com.github.cgks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MotifMiningApplication {
    public static void main(String[] args) {
        SpringApplication.run(MotifMiningApplication.class, args);
    }
}
