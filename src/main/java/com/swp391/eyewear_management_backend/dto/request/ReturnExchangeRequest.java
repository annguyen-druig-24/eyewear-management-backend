package com.swp391.eyewear_management_backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnExchangeRequest {
    

    Long orderDetailId;
    

    Integer quantity;
    
    String returnReason;
    
    String returnType;
    
    String refundMethod;
    
    String refundAccountNumber;
}
