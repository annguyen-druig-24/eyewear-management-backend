package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.integration.vnpay.VnpayService;
import com.swp391.eyewear_management_backend.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    private final VnpayService vnpayService;

    @Override
    public String createPaymentUrl(String method, Long orderId, Long paymentId, BigDecimal amount) {
        String m = method == null ? "" : method.trim().toUpperCase();

        if ("VNPAY".equals(m)) {
            return vnpayService.createPaymentUrl(orderId, paymentId, amount);
        }

        if ("MOMO".equals(m)) {
            // TODO: tích hợp sau
            return "http://localhost:8080/payments/momo/mock?orderId=" + orderId
                    + "&paymentId=" + paymentId + "&amount=" + amount;
        }

        if ("COD".equals(m)) return null;

        return null;
    }
}