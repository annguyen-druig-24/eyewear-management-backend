package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.entity.CartItem;
import com.swp391.eyewear_management_backend.entity.Order;
import com.swp391.eyewear_management_backend.entity.OrderProcessing;
import com.swp391.eyewear_management_backend.entity.User;
import com.swp391.eyewear_management_backend.repository.CartItemRepo;
import com.swp391.eyewear_management_backend.repository.OrderProcessingRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutCartTrackingService {
    private static final ZoneId APP_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String NOTE_PREFIX = "CHECKOUT_CART_ITEM_IDS:";

    private final OrderProcessingRepo orderProcessingRepo;
    private final CartItemRepo cartItemRepo;

    public void recordCheckoutCartItemIds(Order order, User changedBy, List<Long> cartItemIds) {
        if (order == null || changedBy == null || cartItemIds == null || cartItemIds.isEmpty()) {
            return;
        }
        String serialized = cartItemIds.stream()
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        if (serialized.isBlank()) {
            return;
        }
        OrderProcessing op = new OrderProcessing();
        op.setOrder(order);
        op.setChangedBy(changedBy);
        op.setChangedAt(LocalDateTime.now(APP_ZONE_ID));
        op.setNote(NOTE_PREFIX + serialized);
        orderProcessingRepo.save(op);
    }

    public void cleanupTrackedCartItems(Order order) {
        if (order == null || order.getOrderID() == null || order.getUser() == null || order.getUser().getUserId() == null) {
            return;
        }
        var tracked = orderProcessingRepo.findFirstByOrderOrderIDAndNoteStartingWithOrderByOrderProcessingIDDesc(order.getOrderID(), NOTE_PREFIX);
        if (tracked.isEmpty() || tracked.get().getNote() == null) {
            return;
        }
        String raw = tracked.get().getNote().substring(NOTE_PREFIX.length());
        if (raw.isBlank()) {
            return;
        }
        List<Long> ids = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::parseLongSafe)
                .filter(v -> v != null && v > 0)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        List<CartItem> matched = cartItemRepo.findByUserIdAndIdsFetchAll(order.getUser().getUserId(), ids);
        if (!matched.isEmpty()) {
            cartItemRepo.deleteAll(matched);
        }
    }

    private Long parseLongSafe(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }
}
