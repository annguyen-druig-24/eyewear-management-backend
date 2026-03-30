package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.projection.RevenueChartProjection;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepo orderRepository;
    private final OrderDetailRepo orderDetailRepository;
    private final DashboardMapper dashboardMapper;

    @Override
    public List<TopProductResponse> getTopSellingProducts(LocalDate startDateInput, LocalDate endDateInput) {
        if (startDateInput != null && endDateInput != null && startDateInput.isAfter(endDateInput)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc!");
        }

        LocalDateTime endDateTime = (endDateInput != null)
                ? endDateInput.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        LocalDateTime startDateTime = (startDateInput != null)
                ? startDateInput.atStartOfDay()
                : endDateTime.minusDays(6).with(LocalTime.MIN);

        return orderDetailRepository.getTopSellingProducts(startDateTime, endDateTime, PageRequest.of(0, 5))
                .stream()
                .map(dashboardMapper::toTopProductDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TopProductResponse> getTopSellingProductsByType(LocalDate startDateInput, LocalDate endDateInput, String productTypeName) {
        if (startDateInput != null && endDateInput != null && startDateInput.isAfter(endDateInput)) {
            throw new IllegalArgumentException("Start Date must before End Date!");
        }

        LocalDateTime endDateTime = (endDateInput != null)
                ? endDateInput.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        LocalDateTime startDateTime = (startDateInput != null)
                ? startDateInput.atStartOfDay()
                : endDateTime.minusDays(6).with(LocalTime.MIN);

        return orderDetailRepository.getTopSellingProductsByType(startDateTime, endDateTime, productTypeName, PageRequest.of(0, 5))
                .stream()
                .map(dashboardMapper::toTopProductDto)
                .collect(Collectors.toList());
    }

    @Override
    public DashboardResponse getDashboardStatistics(LocalDate startDateInput, LocalDate endDateInput) {

        // =========================================================================
        // 1. VALIDATE: NGÀY BẮT ĐẦU PHẢI TRƯỚC HOẶC BẰNG NGÀY KẾT THÚC
        // =========================================================================
        if (startDateInput != null && endDateInput != null && startDateInput.isAfter(endDateInput)) {
            // Ném ra Exception (Bạn có thể dùng Custom Exception của dự án nếu có)
            throw new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc!");
        }

        // ==========================================================================
        // 2. CHUẨN HÓA THỜI GIAN
        // =========================================================================
        LocalDateTime endDateTime = (endDateInput != null)
                ? endDateInput.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        LocalDateTime startDateTime = (startDateInput != null)
                ? startDateInput.atStartOfDay()
                : endDateTime.minusDays(6).with(LocalTime.MIN); // Mặc định lùi 7 ngày

        // ==========================================================================
        // 3. TÍNH TOÁN Ô SUMMARY (TỔNG QUAN) TỪ NGÀY A -> B
        // ==========================================================================

        // 1. Tính tổng doanh thu ĐÚNG trong khoảng startDateTime đến endDateTime
        BigDecimal totalRev = orderRepository.calculateRevenueBetween(startDateTime, endDateTime);

        // 2. Đếm số đơn Pending trong khoảng start -> end
        int pendingOrders = orderRepository.countByOrderStatusAndOrderDateBetween("PENDING", startDateTime, endDateTime);

        // 3. Đếm số đơn Completed trong khoảng start -> end
        int completedOrders = orderRepository.countByOrderStatusAndOrderDateBetween("COMPLETED", startDateTime, endDateTime);

        // 4. Đóng gói lại
        SummaryResponse summary = SummaryResponse.builder()
                .totalRevenue(totalRev != null ? totalRev.longValue() : 0L)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .build();

        // =========================================================================
        // 4. LẤY DỮ LIỆU BIỂU ĐỒ & TOP SẢN PHẨM TRONG KHOẢNG A -> B
        // =========================================================================

        // --- 4.1. Biểu đồ doanh thu (Có xử lý trám ngày trống bằng 0) ---
        // Lấy dữ liệu thô từ Database (Chỉ chứa những ngày có đơn hàng)
        List<RevenueChartProjection> rawRevenueData = orderRepository.getRevenueChartNative(startDateTime, endDateTime);

        // Chuyển dữ liệu thô thành một Map để tra cứu nhanh (Key: "dd/MM", Value: Doanh thu)
        Map<String, BigDecimal> revenueMap = rawRevenueData.stream()
                .collect(Collectors.toMap(
                        RevenueChartProjection::getLabel,
                        RevenueChartProjection::getRevenue,
                        (existing, replacement) -> existing // Nếu trùng khóa thì giữ nguyên giá trị cũ
                ));

        // Tạo danh sách kết quả chứa TẤT CẢ các ngày từ start đến end
        List<RevenueChartResponse> revenueChart = new ArrayList<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM");

        LocalDate currentDate = startDateTime.toLocalDate();
        LocalDate endDateLocal = endDateTime.toLocalDate();

        // Chạy vòng lặp từ ngày bắt đầu đến ngày kết thúc
        while (!currentDate.isAfter(endDateLocal)) {
            String label = currentDate.format(formatter);

            // Lấy doanh thu từ Map, nếu ngày đó "ế" (không có trong Map) thì gán = 0
            BigDecimal revenue = revenueMap.getOrDefault(label, BigDecimal.ZERO);

            RevenueChartResponse chartItem = new RevenueChartResponse();
            chartItem.setLabel(label);
            chartItem.setRevenue(revenue.longValue());

            revenueChart.add(chartItem);

            // Tăng thêm 1 ngày để tiếp tục vòng lặp
            currentDate = currentDate.plusDays(1);
        }

        // --- 4.2. Biểu đồ trạng thái đơn hàng từ start -> end ---
        List<OrderStatusChartResponse> orderStatusChart = orderRepository.getOrderStatusChart(startDateTime, endDateTime)
                .stream()
                .map(dashboardMapper::toOrderStatusDto)
                .collect(Collectors.toList());

        // --- 4.3. Top 5 Sản phẩm bán chạy từ start -> end ---
        List<TopProductResponse> topProducts = orderDetailRepository.getTopSellingProducts(startDateTime, endDateTime, PageRequest.of(0, 5))
                .stream()
                .map(dashboardMapper::toTopProductDto)
                .collect(Collectors.toList());

        // =========================================================================
        // 5. ĐÓNG GÓI KẾT QUẢ TRẢ VỀ
        // =========================================================================
        return DashboardResponse.builder()
                .summary(summary)
                .revenueChart(revenueChart) // Đã được trám số 0 đầy đủ các ngày
                .orderStatusChart(orderStatusChart)
                .topProducts(topProducts)
                .build();
    }
}
