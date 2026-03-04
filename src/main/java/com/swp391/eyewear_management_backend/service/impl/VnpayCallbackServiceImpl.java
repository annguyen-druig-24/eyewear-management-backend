package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.entity.Invoice;
import com.swp391.eyewear_management_backend.entity.Order;
import com.swp391.eyewear_management_backend.entity.Payment;
import com.swp391.eyewear_management_backend.repository.CartItemRepo;
import com.swp391.eyewear_management_backend.repository.InvoiceRepo;
import com.swp391.eyewear_management_backend.repository.OrderRepo;
import com.swp391.eyewear_management_backend.repository.PaymentRepo;
import com.swp391.eyewear_management_backend.service.VnpayCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VnpayCallbackServiceImpl implements VnpayCallbackService {

    private final PaymentRepo paymentRepo;
    private final OrderRepo orderRepo;
    private final InvoiceRepo invoiceRepo;
    private final CartItemRepo cartItemRepo;

    /*
        3 mục tiêu chính của hàm handleCallback:
        - Xác nhận giao dịch VNPAY có khớp với Payment trong hệ thống không (đúng paymentId, đúng amount)
        - Cập nhật trạng thái Payment (SUCCESS / FAILED) một cách an toàn và idempotent (gọi nhiều lần không bị cập nhật sai)
        - Nếu thành công thì đồng bộ trạng thái Order + Invoice theo paymentPurpose (FULL/DEPOSIT)
        - Và cuối cùng trả về IpResult để controller quyết định phản hồi/redirect.
     */
    @Transactional
    public IpResult handleCallback(Long paymentId, long vnpAmount, String vnpResponseCode) {
        Payment payment = paymentRepo.findByIdForUpdate(paymentId).orElse(null);
        if (payment == null) return IpResult.ORDER_NOT_FOUND;

        // amount check: payment.amount * 100
        /*
            Mục tiêu:
            - Ai đó sửa query params callback (dù chữ ký đã verify ở controller, nhưng ở tầng service vẫn nên có check logic)
            - Lỗi tạo URL thanh toán: amount gửi đi không đúng với amount bạn lưu Payment
            - Tránh việc paymentId đúng nhưng amount bị mismatch → không được phép confirm.
         */
        long expected = payment.getAmount().multiply(new BigDecimal("100")).longValue();
        if (expected != vnpAmount) return IpResult.INVALID_AMOUNT;

        // idempotent : callback có thể đến nhiều lần, không được xử lý lặp
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
