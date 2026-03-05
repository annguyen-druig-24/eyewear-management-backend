package com.swp391.eyewear_management_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusGroupResponse {
    private String groupName;
    private List<String> orderTypes;
    private List<OrderStatusOptionResponse> statuses;
}
