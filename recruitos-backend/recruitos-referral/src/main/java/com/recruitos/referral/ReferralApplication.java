package com.recruitos.referral;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Referral module Spring Boot application
 */
@SpringBootApplication(scanBasePackages = {"com.recruitos.common", "com.recruitos.referral"})
@MapperScan("com.recruitos.referral.mapper")
public class ReferralApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReferralApplication.class, args);
    }
}
