package com.swp391.eyewear_management_backend.dto.request;

import com.swp391.eyewear_management_backend.dto.request.ShippingAddressRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {

    @NotEmpty
    private List<Long> cartItemIds;

    private Long promotionId;

    // shipping info (recipient editable)
    @NotNull(message = "Name is required!")
    private String recipientName;

    @NotNull(message = "Phone is required!")
    private String recipientPhone;

    private String recipientEmail;

    private String note;

    // optional: nếu null => dùng địa chỉ mặc định của User (có codes)
    private ShippingAddressRequest address;

    // COD / VNPAY / MOMO / PAYOS
    @NotNull(message = "Payment Method is required!")
    private String paymentMethod;

    // chỉ dùng khi depositRequired = true
    // DEPOSIT / FULL (nếu user chọn “Thanh toán cọc” hay “Thanh toán toàn bộ”)
    private String payStrategy;

    // chỉ dùng khi paymentMethod = COD nhưng phải cọc
    private String depositPaymentMethod; // VNPAY/MOMO
}