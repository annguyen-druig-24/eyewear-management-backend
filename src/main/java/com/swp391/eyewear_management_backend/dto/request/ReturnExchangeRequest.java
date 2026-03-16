package com.swp391.eyewear_management_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnExchangeRequest {

    // NOT NULL trong DB
    @NotNull(message = "Mã đơn hàng không được để trống")
    Long orderId;

    // NOT NULL trong DB
    @NotBlank(message = "Loại yêu cầu đổi trả (Return Type) không được để trống")
    String returnType; // VD: RETURN, EXCHANGE, WARRANTY, REFUND

    // NOT NULL trong DB
    @NotBlank(message = "Phạm vi yêu cầu (Request Scope) không được để trống")
    String requestScope; // VD: ORDER, ITEM



    String requestNote;

    String returnReason;



    String refundMethod; // VD: BANK_TRANSFER, EWALLET

    String refundAccountNumber;

    String refundAccountName;

    // Danh sách các sản phẩm cần đổi trả (nếu Request_Scope = 'ITEM')
    List<ReturnExchangeItemRequest> items;
}