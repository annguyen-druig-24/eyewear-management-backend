package com.swp391.eyewear_management_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.frontend")
public class FrontendProperties {
    private String baseUrl;
    private String successPath = "/order-success";
    private String failPath = "/payment-result";
}