package com.swp391.eyewear_management_backend.dto.request;

import lombok.Data;

@Data
public class CustomerCancelOrderRequest {
    private String cancelReason;
    private String requestNote;
    private String refundMethod;
    private String refundAccountNumber;
    private String refundAccountName;
}
