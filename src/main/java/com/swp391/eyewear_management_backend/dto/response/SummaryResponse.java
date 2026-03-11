package com.swp391.eyewear_management_backend.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResponse {
    private long revenueDay;
    private long revenueWeek;
    private long revenueMonth;
    private int pendingOrders;
    private int completedOrders;
}
