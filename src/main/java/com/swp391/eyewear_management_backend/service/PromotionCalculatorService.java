package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.response.PromotionCandidateResponse;
import com.swp391.eyewear_management_backend.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionCalculatorService {

    PromotionResult evaluate(List<CartItem> cartItems, BigDecimal subTotal, Long selectedPromotionId);

    record PromotionResult(
            BigDecimal discountAmount,      //số tiền đc giảm
            Long appliedPromotionId,        //xác định promotion nào đc sử dụng
            List<PromotionCandidateResponse> availablePromotions,   //danh sách các promotions khả dụng
            PromotionCandidateResponse recommendedPromotion,        //promotions đề xuất (tốt nhất - tính theo discountAmount lớn nhất)
            // itemId -> itemDiscount (để tính prescriptionAmount sau discount)
            java.util.Map<Long, BigDecimal> itemDiscountMap         //để tính số tiền và trừ thẳng vào từng line item, vì promotions có thể chỉ áp dụng cho các loại item đc chỉ định
    ) {}
}
