package com.swp391.eyewear_management_backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderStatusResponse {

    Long orderId;
    String orderCode;
    String orderStatus;        // PENDING/PAID/...
    String orderType;          // DIRECT_ORDER/MIX_ORDER/...

    BigDecimal subTotal;
    BigDecimal discountAmount;
    BigDecimal shippingFee;
    BigDecimal totalAmount;

    String invoiceStatus;      // UNPAID/PARTIALLY_PAID/PAID/...

    LocalDateTime expectedDeliveryAt;

    List<PaymentStatusResponse> payments;
}
