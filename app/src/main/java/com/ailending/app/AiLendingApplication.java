package com.ailending.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the AI Lending Intelligence Platform.
 */
@SpringBootApplication(scanBasePackages = "com.ailending")
public class AiLendingApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiLendingApplication.class, args);
    }
}
