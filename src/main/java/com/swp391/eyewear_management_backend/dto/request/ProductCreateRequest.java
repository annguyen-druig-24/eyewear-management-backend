package com.swp391.eyewear_management_backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCreateRequest {
    private String sku;
    private String name;
    private Double price;
    private Double costPrice;
    private String description;
    private Boolean allowPreorder = false;
    private Boolean isActive = true;

    // Nhận tên thay vì ID
    private String brandName;
    private String typeName;
    
    // Frame fields (cho Gọng kính)
    private String frameColor;
    private BigDecimal frameTempleLength;
    private BigDecimal frameLensWidth;
    private BigDecimal frameBridgeWidth;
    private String frameShapeName;
    private String frameMaterialName;
    private String frameDescription;
    
    // Lens fields (cho Tròng kính)
    private String lensTypeName;
    private BigDecimal lensIndexValue;
    private BigDecimal lensDiameter;
    private String lensAvailablePowerRange;
    private Boolean lensIsBlueLightBlock;
    private Boolean lensIsPhotochromic;
    private String lensDescription;
    
    // ContactLens fields (cho Kính áp tròng)
    private String contactLensUsageType;
    private BigDecimal contactLensBaseCurve;
    private BigDecimal contactLensDiameter;
    private BigDecimal contactLensWaterContent;
    private String contactLensAvailablePowerRange;
    private Integer contactLensQuantityPerBox;
    private String contactLensMaterial;
    private String contactLensReplacementSchedule;
    private String contactLensColor;
}
