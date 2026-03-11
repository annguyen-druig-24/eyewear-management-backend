package com.swp391.eyewear_management_backend.config.ghn;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ghn")
public class GhnProperties {
    private String baseUrl;
    private String token;
    private Long shopId;

    // điểm gửi (shop) - bạn phải set trong application.yml
    private Integer fromDistrictId;
    private String fromWardCode;

    // chọn loại dịch vụ ưu tiên (2 = "Chuẩn" theo list GHN)
    private Integer preferredServiceTypeId = 2;
    private Integer paymentTypeId = 2;
    private String requiredNote = "CHOPHEPXEMHANG";
    private Integer codMaxAmount = 300000;

    // ===== NEW: package config =========
    private PackageConfig pkg = new PackageConfig();

    @Data
    public static class PackageConfig {
        private int length = 20;
        private int width = 15;
        private int height = 8;
        private WeightConfig weight = new WeightConfig();
    }

    @Data
    public static class WeightConfig {
        private int contactLens = 120;
        private int lens = 220;
        private int frame = 300;
        private int prescription = 450;
        private int def = 250; // "default"
    }
}
