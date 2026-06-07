package com.recruitos.candidate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Candidate module Spring Boot application
 */
@SpringBootApplication(scanBasePackages = {"com.recruitos.common", "com.recruitos.candidate"})
@MapperScan("com.recruitos.candidate.mapper")
public class CandidateApplication {

    public static void main(String[] args) {
        SpringApplication.run(CandidateApplication.class, args);
    }
}
