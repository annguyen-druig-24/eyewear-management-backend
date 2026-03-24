package com.swp391.eyewear_management_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BrandResponse {
    private Long id;
    private String brandName;
    private String description;
    private String logoUrl;
}
