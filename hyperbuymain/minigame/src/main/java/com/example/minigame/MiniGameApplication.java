package com.example.minigame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MiniGameApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiniGameApplication.class, args);
    }
}
