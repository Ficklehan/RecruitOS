package com.recruitos.offer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Offer module Spring Boot application
 */
@SpringBootApplication(scanBasePackages = {"com.recruitos.offer", "com.recruitos.common"})
@MapperScan("com.recruitos.offer.mapper")
public class OfferApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfferApplication.class, args);
    }
}
