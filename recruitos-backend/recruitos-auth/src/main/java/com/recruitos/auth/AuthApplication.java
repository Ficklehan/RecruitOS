package com.recruitos.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auth module Spring Boot application
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.recruitos.common", "com.recruitos.auth"})
@MapperScan("com.recruitos.auth.mapper")
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
