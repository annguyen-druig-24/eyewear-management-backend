package com.swp391.eyewear_management_backend.dto.projection;

import java.math.BigDecimal;

public interface RevenueChartProjection {
    String getLabel();
    BigDecimal getRevenue();
}
