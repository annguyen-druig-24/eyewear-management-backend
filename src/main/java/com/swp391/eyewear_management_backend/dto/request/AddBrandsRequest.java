package com.swp391.eyewear_management_backend.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class AddBrandsRequest {
    private List<BrandDto> brands;
}