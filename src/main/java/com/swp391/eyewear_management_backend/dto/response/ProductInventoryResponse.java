package com.swp391.eyewear_management_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductInventoryResponse {
    private Long productId;
    private String productName;
    private String productTypeName;

    @JsonProperty("SKU")
    private String sku;

    private String brandName;
    private String frameMaterialName;
    private String frameShapeName;
    private String lensTypeName;
    private BigDecimal indexValue;
    private Boolean isBlueLightBlock;
    private Boolean isPhotochromic;
    private String usageType;
    private String lensMaterial;
    private BigDecimal baseCurve;
    private BigDecimal waterContent;
    private String replacementSchedule;
    private Integer onHandQuantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Boolean isActive;
}
