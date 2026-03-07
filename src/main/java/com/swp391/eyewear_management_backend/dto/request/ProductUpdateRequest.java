package com.swp391.eyewear_management_backend.dto.request;

import lombok.Data;

@Data
public class ProductUpdateRequest {
    private Long id;
    private String sku;
    private String name;
    private Double price;
    private String description;

    // Nhận tên thay vì ID
    private String brandName;
    private String typeName;
}
