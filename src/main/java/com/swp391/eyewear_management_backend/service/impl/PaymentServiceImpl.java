package com.swp391.eyewear_management_backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swp391.eyewear_management_backend.dto.request.PaymentRequest;
import com.swp391.eyewear_management_backend.dto.response.PaymentResponse;
import com.swp391.eyewear_management_backend.entity.Order;
import com.swp391.eyewear_management_backend.entity.User;
import com.swp391.eyewear_management_backend.repository.OrderRepo;
import com.swp391.eyewear_management_backend.repository.UserRepo;
import com.swp391.eyewear_management_backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Chỉ import đúng 3 class này của PayOS 2.0.1, tuyệt đối KHÔNG import vn.payos.type...
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PayOS payOS;
    private final OrderRepo orderRepository;
    private final UserRepo userRepo;

    @Override
    public String createPayOSPaymentUrl(Long paymentId, long amount, String orderCodeStr) {
        try {
            // PayOS giới hạn description tối đa 25 ký tự
            String description = "Thanh toan " + orderCodeStr;
            if (description.length() > 25) {
                description = description.substring(0, 25);
            }

            String returnUrl = "http://localhost:5173/success";
            String cancelUrl = "http://localhost:5173/cancel";

            CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                    // Dùng paymentId làm mã đơn của PayOS vì nó là kiểu Long hợp lệ
                    .orderCode(paymentId)
                    .amount(amount)
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .build();

            CreatePaymentLinkResponse checkoutResponse = payOS.paymentRequests().create(paymentRequest);

            return checkoutResponse.getCheckoutUrl();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo link thanh toán PayOS: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse createPaymentLink(PaymentRequest request) {
        try {
            if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Số tiền thanh toán không hợp lệ");
            }

            long payosOrderCode = System.currentTimeMillis() / 1000;
            String dbOrderCode = String.valueOf(payosOrderCode);

            User currentUser = userRepo.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + request.getUserId()));

            Order newOrder = Order.builder()
                    .orderCode(dbOrderCode)
                    .user(currentUser)
                    .subTotal(request.getTotalAmount())
                    .taxAmount(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .orderType("MIX_ORDER")
                    .orderStatus("PENDING")
                    .orderDate(LocalDateTime.now())
                    .build();

            orderRepository.save(newOrder);

            String description = "Kinh mat " + payosOrderCode;
            String returnUrl = "http://localhost:5173/success";
            String cancelUrl = "http://localhost:5173/cancel";

            CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(payosOrderCode)
                    .amount(request.getTotalAmount().setScale(0, RoundingMode.HALF_UP).longValue())
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .build();

            CreatePaymentLinkResponse checkoutResponse = payOS.paymentRequests().create(paymentRequest);

            return new PaymentResponse(checkoutResponse.getCheckoutUrl(), payosOrderCode);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo link thanh toán: " + e.getMessage());
        }
    }

    @Override
    public String checkOrderStatus(Long orderCode) {
        Optional<Order> orderOpt = orderRepository.findByOrderCode(String.valueOf(orderCode));
        if (orderOpt.isPresent()) {
            return orderOpt.get().getOrderStatus();
        }
        return "NOT_FOUND";
    }

    @Override
    @Transactional
    public void processWebhook(ObjectNode webhookBody) {
        try {
            // 🌟 ĐIỂM ĂN TIỀN LÀ ĐÂY: Dùng hàm verify của bản 2.0.1 (Nếu hacker gửi fake, nó sẽ quăng lỗi)
            payOS.webhooks().verify(webhookBody);

            // 🌟 Tự trích xuất data an toàn bằng JsonNode, chia tay luôn class WebhookData
            if (webhookBody.has("code") && "00".equals(webhookBody.get("code").asText())) {
                JsonNode dataNode = webhookBody.get("data");

                if (dataNode != null && dataNode.has("orderCode")) {
                    Long payosOrderCode = dataNode.get("orderCode").asLong();
                    String dbOrderCode = String.valueOf(payosOrderCode);

                    Optional<Order> orderOpt = orderRepository.findByOrderCode(dbOrderCode);
                    if (orderOpt.isPresent()) {
                        Order order = orderOpt.get();
                        order.setOrderStatus("PAID");

                        orderRepository.save(order);
                        System.out.println("✅ Đã cập nhật đơn hàng " + dbOrderCode + " thành PAID");
                    } else {
                        System.err.println("❌ Nhận được tiền nhưng không tìm thấy orderCode: " + dbOrderCode);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi xác thực Webhook: " + e.getMessage());
        }
    }
}