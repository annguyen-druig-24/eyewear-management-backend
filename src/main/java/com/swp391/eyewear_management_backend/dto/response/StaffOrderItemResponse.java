package com.swp391.eyewear_management_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/*
    * Class này dùng để hiển thị thông tin của sản phẩm (DIRECT_ORDER, PRE_ORDER)
*/

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOrderItemResponse {
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String imageUrl;
}
