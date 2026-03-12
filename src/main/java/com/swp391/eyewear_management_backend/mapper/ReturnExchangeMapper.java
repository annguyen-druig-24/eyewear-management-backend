package com.swp391.eyewear_management_backend.mapper;

import com.swp391.eyewear_management_backend.dto.response.ReturnExchangeResponse;
import com.swp391.eyewear_management_backend.entity.ReturnExchange;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReturnExchangeMapper {
    
    @Mapping(source = "returnExchangeID", target = "returnExchangeID")
    @Mapping(source = "returnCode", target = "returnCode")
    @Mapping(source = "orderDetail.orderDetailID", target = "orderDetailId")
    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "quantity", target = "quantity")
    @Mapping(source = "returnReason", target = "returnReason")
    @Mapping(source = "returnType", target = "returnType")
    @Mapping(source = "productCondition", target = "productCondition")
    @Mapping(source = "refundAmount", target = "refundAmount")
    @Mapping(source = "refundMethod", target = "refundMethod")
    @Mapping(source = "refundAccountNumber", target = "refundAccountNumber")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "requestDate", target = "requestDate")
    @Mapping(source = "approvedDate", target = "approvedDate")
    @Mapping(source = "approvedBy.userId", target = "approvedById")
    @Mapping(source = "rejectReason", target = "rejectReason")
    @Mapping(source = "imageUrl", target = "imageUrl")
    ReturnExchangeResponse toReturnExchangeResponse(ReturnExchange returnExchange);
}
