package com.swp391.eyewear_management_backend.dto.projection;

import java.math.BigDecimal;

public interface TopProductProjection {
    Integer getId();
    String getName();
    BigDecimal getPrice();
    Integer getSold();
    String getImage();
}
