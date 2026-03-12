package com.swp391.eyewear_management_backend.mapper;

import com.swp391.eyewear_management_backend.dto.projection.OrderStatusProjection;
import com.swp391.eyewear_management_backend.dto.projection.RevenueChartProjection;
import com.swp391.eyewear_management_backend.dto.projection.TopProductProjection;
import com.swp391.eyewear_management_backend.dto.response.OrderStatusChartResponse;
import com.swp391.eyewear_management_backend.dto.response.RevenueChartResponse;
import com.swp391.eyewear_management_backend.dto.response.TopProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DashboardMapper {

    @Mapping(target = "label", source = "label")
    @Mapping(target = "revenue", source = "revenue")
    RevenueChartResponse toRevenueDto(RevenueChartProjection projection);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "sold", source = "sold")
    @Mapping(target = "image", source = "image")
    TopProductResponse toTopProductDto(TopProductProjection projection);

    @Mapping(target = "name", expression = "java(translateStatus(projection.getStatus()))")
    @Mapping(target = "value", source = "count")
    OrderStatusChartResponse toOrderStatusDto(OrderStatusProjection projection);

    // Hàm tiện ích hỗ trợ dịch trạng thái đơn hàng (Dựa trên data của bạn)
    default String translateStatus(String status) {
        if (status == null) return "Khác";
        switch (status.toUpperCase()) {
            case "PENDING": return "Đang xử lý";
            case "COMPLETED": return "Hoàn thành";
            case "CANCELED": return "Đã hủy";
            case "PAID": return "Đã thanh toán";
            case "PARTIALLY_PAID": return "Thanh toán một phần";
            case "PROCESSING": return "Đang chuẩn bị hàng";
            case "CONFIRMED": return "Đã xác nhận";
            case "READY": return "Chờ giao hàng";
            default: return status;
        }
    }
}
