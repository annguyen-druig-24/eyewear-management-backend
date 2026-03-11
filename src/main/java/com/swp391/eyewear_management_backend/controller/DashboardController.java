package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.response.DashboardResponse;
import com.swp391.eyewear_management_backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboardData() {
        DashboardResponse response = dashboardService.getDashboardStatistics();
        return ResponseEntity.ok(response);
    }
}