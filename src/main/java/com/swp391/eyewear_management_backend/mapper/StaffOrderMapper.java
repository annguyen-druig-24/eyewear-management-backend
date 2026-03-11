package com.swp391.eyewear_management_backend.mapper;

import com.swp391.eyewear_management_backend.dto.response.StaffOrderListResponse;
import com.swp391.eyewear_management_backend.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StaffOrderMapper {
    @Mapping(source = "orderID", target = "orderId")
    @Mapping(source = "user.name", target = "customerName")
    @Mapping(source = "shippingInfo.shippingStatus", target = "shippingStatus")
    StaffOrderListResponse toStaffOrderListResponse(Order order);
}
