package com.swp391.eyewear_management_backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnExchangeItemResponse {
    private Long returnExchangeItemId;
    private Long orderDetailId;
    private Long productId;
    private String productName;
    private Integer requestedQuantity;
    private Integer orderQuantity;
    private String itemSource;
    private String itemEvidenceURL;
    private Long prescriptionOrderDetailId;
    private String itemReason;
    private String note;
}

