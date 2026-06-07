package com.recruitos.interview;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Interview module Spring Boot application
 */
@SpringBootApplication(scanBasePackages = {"com.recruitos.common", "com.recruitos.interview"})
@MapperScan("com.recruitos.interview.mapper")
public class InterviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewApplication.class, args);
    }
}
