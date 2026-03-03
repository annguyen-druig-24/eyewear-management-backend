package com.swp391.eyewear_management_backend.config.vnpay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "vnpay")
public class VnpayProperties {
    private String payUrl;
    private String tmnCode;
    private String hashSecret;
    private String returnUrl;
    private String ipnUrl;

    private String version = "2.1.0";
    private String command = "pay";
    private String currCode = "VND";
    private String locale = "vn";
    private String orderType = "other";
    private Integer expireMinutes = 15;
}
