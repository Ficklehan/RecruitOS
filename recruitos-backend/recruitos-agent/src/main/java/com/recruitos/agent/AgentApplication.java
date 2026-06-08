package com.recruitos.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Agent module Spring Boot application
 */
@SpringBootApplication(scanBasePackages = {"com.recruitos.agent", "com.recruitos.common"})
@MapperScan("com.recruitos.agent.mapper")
@EnableScheduling
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
