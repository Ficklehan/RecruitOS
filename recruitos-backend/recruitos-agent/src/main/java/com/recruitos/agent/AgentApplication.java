package com.recruitos.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Agent module Spring Boot application
 */
@SpringBootApplication(scanBasePackages = {"com.recruitos.agent", "com.recruitos.common"})
@MapperScan("com.recruitos.agent.mapper")
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
