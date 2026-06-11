package com.recruitos.brain;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.recruitos.brain.mapper")
@ComponentScan(basePackages = {"com.recruitos.brain", "com.recruitos.common"})
public class BrainApplication {
    public static void main(String[] args) {
        SpringApplication.run(BrainApplication.class, args);
    }
}
