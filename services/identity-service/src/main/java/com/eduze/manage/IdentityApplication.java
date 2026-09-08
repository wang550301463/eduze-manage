package com.eduze.manage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.eduze.manage", "com.eduze.platform.runtime"})
@ConfigurationPropertiesScan("com.eduze.manage")
@MapperScan(
        basePackages = "com.eduze.manage",
        annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class IdentityApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}
