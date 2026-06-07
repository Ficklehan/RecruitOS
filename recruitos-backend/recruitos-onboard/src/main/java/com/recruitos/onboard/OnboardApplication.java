package com.recruitos.onboard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Onboard module Spring Boot application
 */
@SpringBootApplication(scanBasePackages = {"com.recruitos.onboard", "com.recruitos.common"})
@MapperScan("com.recruitos.onboard.mapper")
public class OnboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnboardApplication.class, args);
    }
}
