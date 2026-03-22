package com.swp391.eyewear_management_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductOfSupplierResponse {
    private Long productID;
    private String productName;
    private String SKU;

    // Trích xuất thông tin cơ bản của Brand và ProductType
    private Long productTypeId;
    private String productTypeName;

    private Long brandId;
    private String brandName;

    private BigDecimal price;
    private BigDecimal costPrice;

    // Thay vì trả nguyên Object Frame/Lens/ContactLens, ta trả về ID của chúng
    // (Nếu giá trị là null nghĩa là sản phẩm đó không phải là gọng/tròng kính tương ứng)
    private Long frameId;
    private Long lensId;
    private Long contactLensId;
}