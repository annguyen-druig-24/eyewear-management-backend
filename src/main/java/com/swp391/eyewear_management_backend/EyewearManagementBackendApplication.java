package com.swp391.eyewear_management_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan(basePackages = "com.swp391.eyewear_management_backend")
public class EyewearManagementBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EyewearManagementBackendApplication.class, args);
    }

}
