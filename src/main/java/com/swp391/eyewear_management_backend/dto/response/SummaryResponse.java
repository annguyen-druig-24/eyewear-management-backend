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
    // Chỉ cần 1 biến tổng doanh thu cho khoảng thời gian A -> B
    private Long totalRevenue;

    private Integer pendingOrders;
    private Integer completedOrders;
}
