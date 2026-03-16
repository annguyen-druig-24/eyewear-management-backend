package com.swp391.eyewear_management_backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnExchangeItemResponse {
    Long returnExchangeItemId;
    Long orderDetailId;
    Integer quantity;
    String itemReason;
    String note;
}