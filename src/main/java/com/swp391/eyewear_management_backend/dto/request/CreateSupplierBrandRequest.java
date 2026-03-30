package com.swp391.eyewear_management_backend.dto.request;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class CreateSupplierBrandRequest {
    // Thông tin Supplier
    @NotNull(message = "Supplier name is required")
    private String supplierName;
    @NotNull(message = "Supplier phone is required")
    private String supplierPhone;
    @NotNull(message = "Supplier address is required")
    private String supplierAddress;


}
