package com.recruitos.communication;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Communication module Spring Boot application
 */
@SpringBootApplication(scanBasePackages = {"com.recruitos.communication", "com.recruitos.common"})
@MapperScan("com.recruitos.communication.mapper")
@EnableScheduling
public class CommunicationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunicationApplication.class, args);
    }
}
