package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.response.DashboardResponse;

import java.time.LocalDate;

public interface DashboardService {
    DashboardResponse getDashboardStatistics(LocalDate startDate, LocalDate endDate);
}
