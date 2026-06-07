package com.recruitos.tenant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Tenant module Spring Boot application
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.recruitos.common", "com.recruitos.tenant"})
@MapperScan("com.recruitos.tenant.mapper")
public class TenantApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantApplication.class, args);
    }
}
