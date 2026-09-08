package com.eduze.engagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.eduze.engagement", "com.eduze.platform.runtime"})
public class EngagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(EngagementApplication.class, args);
    }
}
