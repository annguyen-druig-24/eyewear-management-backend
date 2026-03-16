package com.swp391.eyewear_management_backend.mapper;

import com.swp391.eyewear_management_backend.dto.response.ReturnExchangeItemResponse;
import com.swp391.eyewear_management_backend.dto.response.ReturnExchangeResponse;
import com.swp391.eyewear_management_backend.entity.ReturnExchange;
import com.swp391.eyewear_management_backend.entity.ReturnExchangeItem; // Đảm bảo import đúng entity con
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReturnExchangeMapper {

    // --- 1. Map cho Object chính ---
    @Mapping(source = "order.orderID", target = "orderId") // Giả sử entity ReturnExchange có object Order
    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "approvedBy.userId", target = "approvedById")
    @Mapping(source = "processedBy.userId", target = "processedById")
    // MapStruct sẽ TỰ ĐỘNG map các trường trùng tên (returnCode, status, refundAmount...)
    // và TỰ ĐỘNG gọi hàm map item ở dưới để xử lý List<ReturnExchangeItem> items;
    ReturnExchangeResponse toReturnExchangeResponse(ReturnExchange returnExchange);

    // --- 2. Map cho các Item con bên trong ---
    @Mapping(source = "orderDetail.orderDetailID", target = "orderDetailId") // Giả sử entity ReturnExchangeItem có object OrderDetail
    // Các trường như quantity, note, itemReason trùng tên sẽ tự động map
    ReturnExchangeItemResponse toReturnExchangeItemResponse(ReturnExchangeItem item);
}