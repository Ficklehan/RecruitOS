package com.recruitos.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {"com.recruitos.llm", "com.recruitos.common.exception"},
        exclude = {DataSourceAutoConfiguration.class}
)
public class LlmApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmApplication.class, args);
    }
}
