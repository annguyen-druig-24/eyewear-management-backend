package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.config.FrontendProperties;
import com.swp391.eyewear_management_backend.integration.vnpay.VnpayService;
import com.swp391.eyewear_management_backend.service.PaymentGatewayService;
import com.swp391.eyewear_management_backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/*
    - Dispatcher chọn cổng thanh toán (VNPAY/PAYOS/COD).
    - Khi method=VNPAY thì gọi `vnpayService.createVnpayPaymentUrl(...)`.
*/

@Service
@RequiredArgsConstructor
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    private final VnpayService vnpayService;
    private final PaymentService paymentService;
    private final FrontendProperties frontendProperties;

    /*
        1) Mục đích: gateway router cho nhiều cổng thanh toán.
        2) Dùng ở đâu: `OrderServiceImpl` khi tạo đơn.
        3) Logic nhánh VNPAY:
            - chuẩn hóa method uppercase,
            - nếu `VNPAY` => gọi `vnpayService.createVnpayPaymentUrl(...)`
    */
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

        if ("MOMO".equals(m)) {     //Chưa làm
            // Mock MOMO: redirect về frontend success page thay vì backend
            return frontendProperties.getBaseUrl() + frontendProperties.getSuccessPath() 
                    + "?status=SUCCESS&paymentId=" + paymentId + "&orderId=" + orderId;
        }

        if ("COD".equals(m)) return null;

        return null;
    }
}