package com.swp391.eyewear_management_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.backend")
public class BackendProperties {
    private String baseUrl;
}
