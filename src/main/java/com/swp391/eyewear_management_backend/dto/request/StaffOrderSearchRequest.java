package com.swp391.eyewear_management_backend.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOrderSearchRequest {
    private String orderCode;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    private String orderType;
    private String orderStatus;

    @Builder.Default
    @Min(0)
    private Integer page = 0;

    @Builder.Default
    @Min(1)
    @Max(100)
    private Integer size = 10;

    @Builder.Default
    private String sortBy = "orderDate";

    @Builder.Default
    private String sortDir = "desc";
}