package com.swp391.eyewear_management_backend.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data

public class ProductDetailResponse {
    private String Product_Type;
    private Long id;
    private String name;
    private String sku;
    private BigDecimal price;
    private String brandName;
    private List<String> imageUrls;
    // Thêm trường availableQuantity
    private Integer availableQuantity;
}
