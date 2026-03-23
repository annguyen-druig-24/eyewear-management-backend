package com.swp391.eyewear_management_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTryOnConfigUploadRequest {
    private Boolean isEnabled;
    private String previewImageUrl;
    private BigDecimal scaleValue;
    private BigDecimal offsetX;
    private BigDecimal offsetY;
    private BigDecimal offsetZ;
    private BigDecimal rotationX;
    private BigDecimal rotationY;
    private BigDecimal rotationZ;
    private String notes;
    private String anchorMode;
    private String scaleMode;
    private BigDecimal fitRatio;
    private BigDecimal offsetIpdX;
    private BigDecimal offsetIpdY;
    private BigDecimal offsetIpdZ;
    private BigDecimal yawBias;
    private BigDecimal pitchBias;
    private BigDecimal rollBias;
    private BigDecimal depthRatio;
}