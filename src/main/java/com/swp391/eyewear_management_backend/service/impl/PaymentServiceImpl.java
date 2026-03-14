package com.swp391.eyewear_management_backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swp391.eyewear_management_backend.config.FrontendProperties;
import com.swp391.eyewear_management_backend.config.OrderConstants;
import com.swp391.eyewear_management_backend.dto.request.PaymentRequest;
import com.swp391.eyewear_management_backend.dto.response.PaymentResponse;
import com.swp391.eyewear_management_backend.entity.Invoice;
import com.swp391.eyewear_management_backend.entity.Order;
import com.swp391.eyewear_management_backend.entity.Payment;
import com.swp391.eyewear_management_backend.entity.User;
import com.swp391.eyewear_management_backend.repository.InvoiceRepo;
import com.swp391.eyewear_management_backend.repository.OrderRepo;
import com.swp391.eyewear_management_backend.repository.PaymentRepo;
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
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final ZoneId APP_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PayOS payOS;
    private final OrderRepo orderRepository;
    private final UserRepo userRepo;
    private final PaymentRepo paymentRepo;
    private final InvoiceRepo invoiceRepo;
    private final FrontendProperties frontendProperties;
    private final CheckoutCartTrackingService checkoutCartTrackingService;

    @Override
    public String createPayOSPaymentUrl(Long paymentId, long amount, String orderCodeStr) {
        try {
            // PayOS giới hạn description tối đa 25 ký tự
            String description = "Thanh toan " + orderCodeStr;
            if (description.length() > 25) {
                description = description.substring(0, 25);
            }

            String returnUrl = buildFrontendUrl(frontendProperties.getSuccessPath());
            String cancelUrl = buildFrontendUrl(frontendProperties.getCancelPath());

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
            String returnUrl = buildFrontendUrl(frontendProperties.getSuccessPath());
            String cancelUrl = buildFrontendUrl(frontendProperties.getCancelPath());

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
            payOS.webhooks().verify(webhookBody);

            JsonNode dataNode = webhookBody.get("data");
            if (dataNode == null || !dataNode.has("orderCode")) {
                return;
            }

            Long paymentId = dataNode.get("orderCode").asLong();
            Payment payment = paymentRepo.findByIdForUpdate(paymentId).orElse(null);
            if (payment == null) {
                return;
            }

            if (!"PENDING".equalsIgnoreCase(payment.getStatus())) {
                return;
            }

            String code = webhookBody.has("code") ? webhookBody.get("code").asText() : null;
            boolean success = "00".equals(code);

            payment.setPaymentDate(LocalDateTime.now(APP_ZONE_ID));
            payment.setStatus(success ? "SUCCESS" : "FAILED");
            paymentRepo.save(payment);

            Order order = payment.getOrder();
            if (order == null) {
                return;
            }

            Invoice invoice = invoiceRepo.findByOrderOrderID(order.getOrderID()).orElse(null);

            if (success) {
                if ("DEPOSIT".equalsIgnoreCase(payment.getPaymentPurpose())) {
                    order.setOrderStatus(OrderConstants.ORDER_STATUS_PARTIALLY_PAID);
                    if (invoice != null) {
                        invoice.setStatus(OrderConstants.INVOICE_STATUS_PARTIALLY_PAID);
                        invoiceRepo.save(invoice);
                    }
                    checkoutCartTrackingService.cleanupTrackedCartItems(order);
                } else {
                    order.setOrderStatus(OrderConstants.ORDER_STATUS_PAID);
                    if (invoice != null) {
                        invoice.setStatus(OrderConstants.INVOICE_STATUS_PAID);
                        invoiceRepo.save(invoice);
                    }
                    checkoutCartTrackingService.cleanupTrackedCartItems(order);
                }
            } else {
                order.setOrderStatus(OrderConstants.ORDER_STATUS_CANCELED);
                if (invoice != null) {
                    invoice.setStatus(OrderConstants.INVOICE_STATUS_CANCELED);
                    invoiceRepo.save(invoice);
                }

                var remainingPayments = paymentRepo.findByOrderIdAndPurposeAndStatusForUpdate(
                        order.getOrderID(),
                        OrderConstants.PAYMENT_PURPOSE_REMAINING,
                        OrderConstants.PAYMENT_STATUS_PENDING
                );
                if (!remainingPayments.isEmpty()) {
                    remainingPayments.forEach(p -> {
                        p.setStatus(OrderConstants.PAYMENT_STATUS_CANCELED);
                        p.setPaymentDate(LocalDateTime.now(APP_ZONE_ID));
                    });
                    paymentRepo.saveAll(remainingPayments);
                }
            }
            orderRepository.save(order);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xử lý webhook PayOS: " + e.getMessage(), e);
        }
    }

    private String buildFrontendUrl(String path) {
        String base = frontendProperties.getBaseUrl();
        String safePath = (path == null || path.isBlank()) ? "/" : path;

        if (base.endsWith("/") && safePath.startsWith("/")) {
            return base.substring(0, base.length() - 1) + safePath;
        }
        if (!base.endsWith("/") && !safePath.startsWith("/")) {
            return base + "/" + safePath;
        }
        return base + safePath;
    }
}
