package com.swp391.eyewear_management_backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnExchangeItemRequest {
    // NOT NULL trong DB
    @NotNull(message = "ID chi tiết sản phẩm không được để trống")
    Long orderDetailId;

    // NOT NULL trong DB
    @NotNull(message = "Số lượng đổi trả không được để trống")
    @Min(value = 1, message = "Số lượng đổi trả phải lớn hơn hoặc bằng 1")
    Integer quantity;

    String itemReason;
    String note;
}