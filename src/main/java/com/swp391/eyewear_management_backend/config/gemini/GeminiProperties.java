package com.swp391.eyewear_management_backend.config.gemini;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.gemini")
public class GeminiProperties {
    private boolean enabled = false;
    private String apiKey = "";
    private String model = "gemini-2.5-flash";
    private String baseUrl = "https://generativelanguage.googleapis.com";
    private int timeoutSeconds = 20;
}
