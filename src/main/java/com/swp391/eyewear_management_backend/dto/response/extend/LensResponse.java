package com.swp391.eyewear_management_backend.dto.response.extend;

import com.swp391.eyewear_management_backend.dto.response.ProductDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class LensResponse extends ProductDetailResponse {
    private Long lensId;
    private String Description;
    private BigDecimal indexValue;
    private BigDecimal diameter;
    private String availablePowerRange;
    private Boolean isBlueLightBlock;
    private Boolean isPhotochromic;
    private String lensTypeName;
    private List<ProductResponse> relatedLenses;
    private List<ProductResponse> relatedFrames;
}
