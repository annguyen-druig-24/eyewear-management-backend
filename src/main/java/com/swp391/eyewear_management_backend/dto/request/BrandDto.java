package com.swp391.eyewear_management_backend.dto.request;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class BrandDto {
    @NotNull(message = "Brand name is required")
    private String brandName;
    private String description;
    private MultipartFile logoFile;
    private Boolean status;
}
