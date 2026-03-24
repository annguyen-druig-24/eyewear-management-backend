package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.response.DashboardResponse;
import com.swp391.eyewear_management_backend.dto.response.TopProductResponse;
import com.swp391.eyewear_management_backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboardData(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        // Truyền 2 ngày này xuống cho Service xử lý
        DashboardResponse response = dashboardService.getDashboardStatistics(startDate, endDate);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductResponse>> getTopSellingProducts(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        List<TopProductResponse> response = dashboardService.getTopSellingProducts(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    
}