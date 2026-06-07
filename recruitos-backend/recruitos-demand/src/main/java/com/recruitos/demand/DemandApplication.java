package com.recruitos.demand;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Demand module Spring Boot application
 */
@SpringBootApplication(scanBasePackages = {"com.recruitos.common", "com.recruitos.demand"})
@MapperScan("com.recruitos.demand.mapper")
public class DemandApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemandApplication.class, args);
    }
}
