package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.entity.Order;
import com.swp391.eyewear_management_backend.entity.Payment;
import com.swp391.eyewear_management_backend.entity.ShippingInfo;
import com.swp391.eyewear_management_backend.entity.User;
import com.swp391.eyewear_management_backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEmailNotificationService {

    private final EmailService emailService;

    public void sendOrderSuccessEmailSafely(Order order, Payment payment) {
        if (order == null) {
            return;
        }

        ShippingInfo shippingInfo = order.getShippingInfo();
        User user = order.getUser();

        String toEmail = firstNonBlank(
                shippingInfo != null ? shippingInfo.getRecipientEmail() : null,
                user != null ? user.getEmail() : null
        );
        if (!StringUtils.hasText(toEmail)) {
            log.warn("Skip order success email because recipient email is missing. orderId={}, paymentId={}",
                    order.getOrderID(),
                    payment != null ? payment.getPaymentID() : null);
            return;
        }

        String customerName = firstNonBlank(
                shippingInfo != null ? shippingInfo.getRecipientName() : null,
                user != null ? user.getName() : null
        );

        BigDecimal amount = order.getTotalAmount() != null
                ? order.getTotalAmount()
                : payment != null ? payment.getAmount() : BigDecimal.ZERO;

        try {
            emailService.sendOrderSuccessEmail(
                    toEmail,
                    customerName,
                    order.getOrderCode(),
                    amount.doubleValue()
            );
        } catch (Exception ex) {
            log.error("Failed to send order success email. orderId={}, paymentId={}",
                    order.getOrderID(),
                    payment != null ? payment.getPaymentID() : null,
                    ex);
        }
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }
}
