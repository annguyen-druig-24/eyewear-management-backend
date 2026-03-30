package com.swp391.eyewear_management_backend.dto.request;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class BrandDto {
    @NotNull(message = "Brand name is required")
    private String brandName;
    private String description;
    private Boolean status;
}
