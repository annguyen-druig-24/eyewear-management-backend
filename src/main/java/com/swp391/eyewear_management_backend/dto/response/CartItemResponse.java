package com.swp391.eyewear_management_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemResponse {

    private Long cartItemId;
    private Long cartId;

    // Thêm trường ItemType (Có thể dùng String hoặc Enum ItemType)
    private String itemType;

    private Long contactLensId;
    private String contactLensName;
    private Double contactLensPrice;
    private String contactLensImg;
    private Integer contactLensAvailableQuantity; // Thêm mới
    private Boolean contactLensPreorder;          // Thêm mới

    private Long frameId;
    private String frameName;
    private Double framePrice;
    private String frameImg;
    private Integer frameAvailableQuantity;       // Thêm mới
    private Boolean framePreorder;                // Thêm mới

    private Long lensId;
    private String lensName;
    private Double lensPrice;
    private String lensImg;
    private Integer lensAvailableQuantity;        // Thêm mới
    private Boolean lensPreorder;                 // Thêm mới

    private Integer quantity;


    private Double price;

    // Prescription information
    private PrescriptionResponse prescription;
}
