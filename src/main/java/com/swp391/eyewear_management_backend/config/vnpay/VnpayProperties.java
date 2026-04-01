package com.swp391.eyewear_management_backend.config.vnpay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/*
    - Binding cấu hình `vnpay.*` thành Java object để service/controller sử dụng.
    1) Dùng để làm gì: map cấu hình `vnpay.*` từ YAML sang object Java.
    2) Được dùng ở đâu: inject vào `VnpayService` và `VnpayController`.
    3) Quy trình: Spring Boot tự bind properties lúc startup.
    4) Logic: field nào không cấu hình thì lấy default (version, command, currCode, locale...).
*/

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
