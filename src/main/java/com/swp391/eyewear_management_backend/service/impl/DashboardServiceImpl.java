package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.response.*;
import com.swp391.eyewear_management_backend.mapper.DashboardMapper;
import com.swp391.eyewear_management_backend.repository.OrderDetailRepo;
import com.swp391.eyewear_management_backend.repository.OrderRepo;
import com.swp391.eyewear_management_backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepo orderRepository;
    private final OrderDetailRepo orderDetailRepository;
    private final DashboardMapper dashboardMapper;

    @Override
    public DashboardResponse getDashboardStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.with(LocalTime.MIN);
        LocalDateTime startOfWeek = now.with(DayOfWeek.MONDAY).with(LocalTime.MIN);
        LocalDateTime startOfMonth = now.withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime startOf7DaysAgo = now.minusDays(6).with(LocalTime.MIN);

        // 1. Tính toán Summary thực tế
        BigDecimal revDay = orderRepository.calculateRevenueBetween(startOfDay, now);
        BigDecimal revWeek = orderRepository.calculateRevenueBetween(startOfWeek, now);
        BigDecimal revMonth = orderRepository.calculateRevenueBetween(startOfMonth, now);

        int pendingOrders = orderRepository.countByOrderStatus("PENDING");
        int completedOrdersMonth = orderRepository.countByOrderStatusAndOrderDateBetween("COMPLETED", startOfMonth, now);

        SummaryResponse summary = SummaryResponse.builder()
                .revenueDay(revDay != null ? revDay.longValue() : 0L)
                .revenueWeek(revWeek != null ? revWeek.longValue() : 0L)
                .revenueMonth(revMonth != null ? revMonth.longValue() : 0L)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrdersMonth)
                .build();

        // 2. Lấy dữ liệu Biểu đồ doanh thu 7 ngày gần nhất
        List<RevenueChartResponse> revenueChart = orderRepository.getRevenueChartNative(startOf7DaysAgo)
                .stream()
                .map(dashboardMapper::toRevenueDto)
                .collect(Collectors.toList());

        // 3. Lấy dữ liệu Biểu đồ trạng thái đơn hàng (Sử dụng MapStruct kèm Auto Translate)
        List<OrderStatusChartResponse> orderStatusChart = orderRepository.getOrderStatusChart()
                .stream()
                .map(dashboardMapper::toOrderStatusDto)
                .collect(Collectors.toList());

        // 4. Lấy Top 3 Sản phẩm bán chạy (Dùng PageRequest để Limit LIMIT 3)
        List<TopProductResponse> topProducts = orderDetailRepository.getTopSellingProducts(PageRequest.of(0, 3))
                .stream()
                .map(dashboardMapper::toTopProductDto)
                .collect(Collectors.toList());

        // Đóng gói DTO tổng
        return DashboardResponse.builder()
                .summary(summary)
                .revenueChart(revenueChart)
                .orderStatusChart(orderStatusChart)
                .topProducts(topProducts)
                .build();
    }
}