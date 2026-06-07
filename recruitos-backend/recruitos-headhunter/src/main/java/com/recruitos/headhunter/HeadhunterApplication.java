package com.recruitos.headhunter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Headhunter module Spring Boot application
 */
@SpringBootApplication(scanBasePackages = {"com.recruitos.common", "com.recruitos.headhunter"})
@MapperScan("com.recruitos.headhunter.mapper")
public class HeadhunterApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeadhunterApplication.class, args);
    }
}
