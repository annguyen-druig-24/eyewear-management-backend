package com.swp391.eyewear_management_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/*
    * Class này dùng để hiển thị thông tin của sản phẩm (PRESCRIPTION_ORDER)
*/

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPrescriptionOrderItemResponse {
    private Long frameId;
    private String frameName;
    private BigDecimal framePrice;
    private String frameImg;
    private Long lensId;
    private String lensName;
    private BigDecimal lensPrice;
    private String lensImg;
    private Long contactLensId;
    private String contactLensName;
    private BigDecimal contactLensPrice;
    private String contactLensImg;
    private String rightEyeSph;
    private String rightEyeCyl;
    private String rightEyeAxis;
    private String leftEyeSph;
    private String leftEyeCyl;
    private String leftEyeAxis;
    private Integer quantity;
    private BigDecimal totalPrice;
}
