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
public class DashboardResponse {
    private SummaryResponse summary;
    private List<RevenueChartResponse> revenueChart;
    private List<OrderStatusChartResponse> orderStatusChart;
    private List<TopProductResponse> topProducts;
}
