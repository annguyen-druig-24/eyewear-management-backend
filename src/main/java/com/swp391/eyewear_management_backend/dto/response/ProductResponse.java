package com.swp391.eyewear_management_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {
    private Long id;
    private String name;
    private String sku;
    private String description;
    private Double price;
    private Boolean allowPreorder;
    private Boolean isActive;
    private String Image_URL;
    private String Brand;
    private String Product_Type;
    private Long frameId;
    private Long lensId;
    private Long contactLensId;
}
