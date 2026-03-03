package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.entity.Invoice;
import com.swp391.eyewear_management_backend.entity.Order;
import com.swp391.eyewear_management_backend.entity.Payment;
import com.swp391.eyewear_management_backend.repository.InvoiceRepo;
import com.swp391.eyewear_management_backend.repository.OrderRepo;
import com.swp391.eyewear_management_backend.repository.PaymentRepo;
import com.swp391.eyewear_management_backend.service.VnpayCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VnpayCallbackServiceImpl implements VnpayCallbackService {

    private final PaymentRepo paymentRepo;
    private final OrderRepo orderRepo;
    private final InvoiceRepo invoiceRepo;

    @Transactional
    public IpResult handleCallback(Long paymentId, long vnpAmount, String vnpResponseCode) {
        Payment payment = paymentRepo.findById(paymentId).orElse(null);
        if (payment == null) return IpResult.ORDER_NOT_FOUND;

        // amount check: payment.amount * 100
        long expected = payment.getAmount().multiply(new BigDecimal("100")).longValue();
        if (expected != vnpAmount) return IpResult.INVALID_AMOUNT;

        // idempotent
        if (!"PENDING".equalsIgnoreCase(payment.getStatus())) {
            return IpResult.ALREADY_CONFIRMED;
        }

        boolean success = "00".equals(vnpResponseCode); // VNPAY: 00 = success :contentReference[oaicite:3]{index=3}

        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus(success ? "SUCCESS" : "FAILED");
        paymentRepo.save(payment);

        // Update Order + Invoice theo purpose
        Order order = payment.getOrder();
        if (order != null) {
            if (success) {
                if ("FULL".equalsIgnoreCase(payment.getPaymentPurpose())) {
                    order.setOrderStatus("PAID");
                    orderRepo.save(order);

                    Invoice inv = invoiceRepo.findByOrderOrderID(order.getOrderID()).orElse(null);
                    if (inv != null) {
                        inv.setStatus("PAID");
                        invoiceRepo.save(inv);
                    }
                } else if ("DEPOSIT".equalsIgnoreCase(payment.getPaymentPurpose())) {
                    order.setOrderStatus("PARTIALLY_PAID");
                    orderRepo.save(order);

                    Invoice inv = invoiceRepo.findByOrderOrderID(order.getOrderID()).orElse(null);
                    if (inv != null) {
                        inv.setStatus("PARTIALLY_PAID");
                        invoiceRepo.save(inv);
                    }
                }
            }
        }

        return success ? IpResult.CONFIRM_SUCCESS : IpResult.CONFIRM_FAILED;
    }
}
