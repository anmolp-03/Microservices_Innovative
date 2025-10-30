package com.example.menu;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbit
public class MenuApplication {
    public static void main(String[] args) {
        SpringApplication.run(MenuApplication.class, args);
    }
}
