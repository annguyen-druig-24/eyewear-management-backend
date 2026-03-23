package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.response.DashboardResponse;
import com.swp391.eyewear_management_backend.dto.response.TopProductResponse;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    DashboardResponse getDashboardStatistics(LocalDate startDate, LocalDate endDate);

    List<TopProductResponse> getTopSellingProducts(LocalDate startDate, LocalDate endDate);
}
