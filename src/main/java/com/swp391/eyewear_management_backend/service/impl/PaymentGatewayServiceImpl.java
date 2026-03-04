package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.integration.vnpay.VnpayService;
import com.swp391.eyewear_management_backend.service.PaymentGatewayService;
import com.swp391.eyewear_management_backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/*
    PaymentGatewayService là interface định nghĩa “những việc liên quan đến thanh toán online mà hệ thống cần”, ví dụ kiểu như:
    - Tạo URL thanh toán (redirect link) cho VNPAY / MOMO / PAYOS
    - (Có thể) tạo payload để frontend gọi/redirect
    - (Có thể) chuẩn hóa tên phương thức thanh toán
    - (Có thể) xử lý logic chung như validate method, mapping paymentPurpose…
    ==> Nói đơn giản: OrderService chỉ gọi PaymentGatewayService, chứ không gọi trực tiếp VnpayService, MomoService, PayosService.
 */

@Service
@RequiredArgsConstructor
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    private final VnpayService vnpayService;
    private final PaymentService paymentService;

    @Override
    public String createPaymentUrl(String method, Long orderId, Long paymentId, BigDecimal amount) {
        String m = method == null ? "" : method.trim().toUpperCase();

        if ("VNPAY".equals(m)) {
            return vnpayService.createVnpayPaymentUrl(orderId, paymentId, amount);
        }

        if ("PAYOS".equals(m)) {
            long payosAmount = (amount == null ? BigDecimal.ZERO : amount)
                    .setScale(0, RoundingMode.HALF_UP)   // PayOS amount là số nguyên
                    .longValue();
            // orderCodeStr có thể dùng orderId cho đơn giản (hoặc order.getOrderCode nếu có sẵn)
            String orderCodeStr = String.valueOf(orderId);
            return paymentService.createPayOSPaymentUrl(paymentId, payosAmount, orderCodeStr);
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