package com.swp391.eyewear_management_backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnExchangeDecisionRequest {
    private String action;
    private String rejectReason;
}