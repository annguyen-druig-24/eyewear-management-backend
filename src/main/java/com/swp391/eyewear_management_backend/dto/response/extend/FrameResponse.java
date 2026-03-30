package com.swp391.eyewear_management_backend.dto.response.extend;

import com.swp391.eyewear_management_backend.dto.response.ProductDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class FrameResponse extends ProductDetailResponse {
    private Long frameId;
    private String Description;
    private String color;
    private String material;
    private String frameShape;
    private BigDecimal templeLength;
    private BigDecimal lensWidth;
    private BigDecimal bridgeWidth;
    private List<ProductResponse> relatedLenses;
    private List<ProductResponse> relatedFrames;

}
