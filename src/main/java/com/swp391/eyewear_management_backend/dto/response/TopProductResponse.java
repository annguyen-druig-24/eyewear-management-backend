package com.swp391.eyewear_management_backend.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductResponse {
    private int id;
    private String name;
    private long price;
    private int sold;
    private String image;
}
