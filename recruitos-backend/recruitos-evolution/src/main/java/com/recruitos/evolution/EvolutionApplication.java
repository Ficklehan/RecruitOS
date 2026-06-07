package com.recruitos.evolution;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Evolution engine module Spring Boot application
 */
@SpringBootApplication(scanBasePackages = {"com.recruitos.common", "com.recruitos.evolution"})
@MapperScan("com.recruitos.evolution.mapper")
public class EvolutionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvolutionApplication.class, args);
    }
}
