package com.swp391.eyewear_management_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReturnExchangeResponse {
    
    @JsonProperty("return_exchange_id")
    Long returnExchangeID;
    
    @JsonProperty("return_code")
    String returnCode;
    
    @JsonProperty("order_detail_id")
    Long orderDetailId;
    
    @JsonProperty("user_id")
    Long userId;
    
    @JsonProperty("quantity")
    Integer quantity;
    
    @JsonProperty("return_reason")
    String returnReason;
    
    @JsonProperty("return_type")
    String returnType;
    
    @JsonProperty("product_condition")
    String productCondition;
    
    @JsonProperty("refund_amount")
    BigDecimal refundAmount;
    
    @JsonProperty("refund_method")
    String refundMethod;
    
    @JsonProperty("refund_account_number")
    String refundAccountNumber;
    
    @JsonProperty("status")
    String status;
    
    @JsonProperty("request_date")
    LocalDateTime requestDate;
    
    @JsonProperty("approved_date")
    LocalDateTime approvedDate;
    
    @JsonProperty("approved_by_id")
    Long approvedById;
    
    @JsonProperty("reject_reason")
    String rejectReason;
    
    @JsonProperty("image_url")
    String imageUrl;
}
