package com.swp391.eyewear_management_backend.dto.response.extend;

import com.swp391.eyewear_management_backend.dto.response.ProductDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class ContactLensResponse extends ProductDetailResponse {
    private Long contactLensId;
    private String Description;
    private String usageType;
    private BigDecimal baseCurve;
    private BigDecimal diameter;
    private BigDecimal waterContent;
    private String availablePowerRange;
    private Integer quantityPerBox;
    private String lensMaterial;
    private String replacementSchedule;
    private String color;
    private List<ProductResponse> relatedContactLenses;
}
