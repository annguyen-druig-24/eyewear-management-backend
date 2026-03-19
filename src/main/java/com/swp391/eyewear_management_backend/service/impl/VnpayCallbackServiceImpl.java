package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.config.OrderConstants;
import com.swp391.eyewear_management_backend.entity.InventoryTransaction;
import com.swp391.eyewear_management_backend.entity.Invoice;
import com.swp391.eyewear_management_backend.entity.Order;
import com.swp391.eyewear_management_backend.entity.OrderDetail;
import com.swp391.eyewear_management_backend.entity.Payment;
import com.swp391.eyewear_management_backend.entity.PrescriptionOrderDetail;
import com.swp391.eyewear_management_backend.entity.Product;
import com.swp391.eyewear_management_backend.entity.ShippingInfo;
import com.swp391.eyewear_management_backend.repository.InventoryTransactionRepo;
import com.swp391.eyewear_management_backend.repository.InvoiceRepo;
import com.swp391.eyewear_management_backend.repository.OrderDetailRepo;
import com.swp391.eyewear_management_backend.repository.OrderRepo;
import com.swp391.eyewear_management_backend.repository.PaymentRepo;
import com.swp391.eyewear_management_backend.repository.PrescriptionOrderRepo;
import com.swp391.eyewear_management_backend.repository.ProductRepo;
import com.swp391.eyewear_management_backend.repository.ShippingInfoRepo;
import com.swp391.eyewear_management_backend.service.VnpayCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VnpayCallbackServiceImpl implements VnpayCallbackService {
    private static final ZoneId APP_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentRepo paymentRepo;
    private final OrderRepo orderRepo;
    private final InvoiceRepo invoiceRepo;
    private final ShippingInfoRepo shippingInfoRepo;
//    private final ProductRepo productRepo;
//    private final OrderDetailRepo orderDetailRepo;
//    private final PrescriptionOrderRepo prescriptionOrderRepo;
//    private final InventoryTransactionRepo inventoryTransactionRepo;
    private final CheckoutCartTrackingService checkoutCartTrackingService;

    /*
        3 mục tiêu chính của hàm handleCallback:
        - Xác nhận giao dịch VNPAY có khớp với Payment trong hệ thống không (đúng paymentId, đúng amount)
        - Cập nhật trạng thái Payment (SUCCESS / FAILED) một cách an toàn và idempotent (gọi nhiều lần không bị cập nhật sai)
        - Nếu thành công thì đồng bộ trạng thái Order + Invoice theo paymentPurpose (FULL/DEPOSIT)
        - Và cuối cùng trả về IpResult để controller quyết định phản hồi/redirect.
     */
    @Transactional
    public IpResult handleCallback(Long paymentId, long vnpAmount, String vnpResponseCode, String vnpTransactionStatus) {
        Payment payment = paymentRepo.findByIdForUpdate(paymentId).orElse(null);
        if (payment == null) return IpResult.ORDER_NOT_FOUND;

        // amount check: payment.amount * 100
        /*
            Mục tiêu:
            - Ai đó sửa query params callback (dù chữ ký đã verify ở controller, nhưng ở tầng service vẫn nên có check logic)
            - Lỗi tạo URL thanh toán: amount gửi đi không đúng với amount bạn lưu Payment
            - Tránh việc paymentId đúng nhưng amount bị mismatch → không được phép confirm.
         */
        long expected = (payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount())
                .setScale(0, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .longValue();
        if (expected != vnpAmount) return IpResult.INVALID_AMOUNT;

        // idempotent : callback có thể đến nhiều lần, không được xử lý lặp
        if (!OrderConstants.PAYMENT_STATUS_PENDING.equalsIgnoreCase(payment.getStatus())) {
            return IpResult.ALREADY_CONFIRMED;
        }

        //boolean success = "00".equals(vnpResponseCode); // VNPAY: 00 = success :contentReference[oaicite:3]{index=3}
        boolean success = "00".equals(vnpResponseCode) && "00".equals(vnpTransactionStatus); // chỉ thành công khi cả responseCode và transactionStatus đều = 00

        payment.setPaymentDate(LocalDateTime.now(APP_ZONE_ID));
        payment.setStatus(success ? OrderConstants.PAYMENT_STATUS_SUCCESS : OrderConstants.PAYMENT_STATUS_FAILED);
        paymentRepo.save(payment);

        // Update Order + Invoice theo purpose
        Order order = payment.getOrder();
        if (order != null) {
            if (success) {
                //finalizeReservedInventoryForOrder(order);
                if ("FULL".equalsIgnoreCase(payment.getPaymentPurpose())) {
                    order.setOrderStatus(OrderConstants.ORDER_STATUS_PAID);
                    orderRepo.save(order);

                    Invoice inv = invoiceRepo.findByOrderOrderID(order.getOrderID()).orElse(null);
                    if (inv != null) {
                        inv.setStatus(OrderConstants.INVOICE_STATUS_PAID);
                        invoiceRepo.save(inv);
                    }
                    checkoutCartTrackingService.cleanupTrackedCartItems(order);
                } else if ("DEPOSIT".equalsIgnoreCase(payment.getPaymentPurpose())) {
                    order.setOrderStatus(OrderConstants.ORDER_STATUS_PARTIALLY_PAID);
                    orderRepo.save(order);

                    Invoice inv = invoiceRepo.findByOrderOrderID(order.getOrderID()).orElse(null);
                    if (inv != null) {
                        inv.setStatus(OrderConstants.INVOICE_STATUS_PARTIALLY_PAID);
                        invoiceRepo.save(inv);
                    }
                    checkoutCartTrackingService.cleanupTrackedCartItems(order);
                }
            } else {
//                releaseReservedInventoryForOrder(order);
                order.setOrderStatus(OrderConstants.ORDER_STATUS_CANCELED);
                orderRepo.save(order);

                ShippingInfo si = order.getShippingInfo();
                if (si != null) {
                    si.setShippingStatus(OrderConstants.SHIPPING_STATUS_CANCELED);
                    shippingInfoRepo.save(si);
                }

                Invoice inv = invoiceRepo.findByOrderOrderID(order.getOrderID()).orElse(null);
                if (inv != null) {
                    inv.setStatus(OrderConstants.INVOICE_STATUS_CANCELED);
                    invoiceRepo.save(inv);
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
        }

        return success ? IpResult.CONFIRM_SUCCESS : IpResult.CONFIRM_FAILED;
    }
//
//    private void finalizeReservedInventoryForOrder(Order order) {
//        if (order == null || order.getOrderID() == null) {
//            return;
//        }
//        Map<Long, Integer> requiredByProductId = aggregateOrderProductQuantities(order.getOrderID());
//        if (requiredByProductId.isEmpty()) {
//            return;
//        }
//        List<Long> productIds = requiredByProductId.keySet().stream().sorted().toList();
//        List<Product> products = productRepo.findByIdsForUpdate(productIds);
//        if (products.size() != productIds.size()) {
//            return;
//        }
//        List<InventoryTransaction> transactions = new ArrayList<>();
//        for (Product product : products) {
//            int requestedQty = requiredByProductId.getOrDefault(product.getProductID(), 0);
//            if (requestedQty <= 0) {
//                continue;
//            }
//            int reservedBefore = product.getReservedQuantity() == null ? 0 : product.getReservedQuantity();
//            int deductedQty = Math.min(reservedBefore, requestedQty);
//            if (deductedQty <= 0) {
//                continue;
//            }
//            int onHandBefore = product.getOnHandQuantity() == null ? 0 : product.getOnHandQuantity();
//            int onHandAfter = Math.max(onHandBefore - deductedQty, 0);
//            product.setOnHandQuantity(onHandAfter);
//            product.setReservedQuantity(reservedBefore - deductedQty);
//
//            InventoryTransaction tx = new InventoryTransaction();
//            tx.setProduct(product);
//            tx.setTransactionType("SALE_OUT");
//            tx.setQuantityBefore(onHandBefore);
//            tx.setQuantityAfter(onHandAfter);
//            tx.setQuantityChange(-deductedQty);
//            tx.setReferenceType("ORDER");
//            tx.setReferenceID(order.getOrderID());
//            tx.setOrder(order);
//            tx.setPerformedBy(order.getUser());
//            tx.setPerformedAt(LocalDateTime.now(APP_ZONE_ID));
//            tx.setNote(order.getOrderCode());
//            transactions.add(tx);
//        }
//        productRepo.saveAll(products);
//        if (!transactions.isEmpty()) {
//            inventoryTransactionRepo.saveAll(transactions);
//        }
//    }
//
//    private void releaseReservedInventoryForOrder(Order order) {
//        if (order == null || order.getOrderID() == null) {
//            return;
//        }
//        Map<Long, Integer> requiredByProductId = aggregateOrderProductQuantities(order.getOrderID());
//        if (requiredByProductId.isEmpty()) {
//            return;
//        }
//        List<Long> productIds = requiredByProductId.keySet().stream().sorted().toList();
//        List<Product> products = productRepo.findByIdsForUpdate(productIds);
//        if (products.size() != productIds.size()) {
//            return;
//        }
//        for (Product product : products) {
//            int requestedQty = requiredByProductId.getOrDefault(product.getProductID(), 0);
//            if (requestedQty <= 0) {
//                continue;
//            }
//            int reservedBefore = product.getReservedQuantity() == null ? 0 : product.getReservedQuantity();
//            int releaseQty = Math.min(reservedBefore, requestedQty);
//            if (releaseQty <= 0) {
//                continue;
//            }
//            product.setReservedQuantity(reservedBefore - releaseQty);
//        }
//        productRepo.saveAll(products);
//    }
//
//    private Map<Long, Integer> aggregateOrderProductQuantities(Long orderId) {
//        Map<Long, Integer> qtyByProductId = new HashMap<>();
//        List<OrderDetail> orderDetails = orderDetailRepo.findByOrderIdFetchProduct(orderId);
//        for (OrderDetail orderDetail : orderDetails) {
//            if (orderDetail == null || orderDetail.getProduct() == null || orderDetail.getProduct().getProductID() == null) {
//                continue;
//            }
//            int quantity = orderDetail.getQuantity() == null ? 0 : orderDetail.getQuantity();
//            if (quantity <= 0) {
//                continue;
//            }
//            qtyByProductId.merge(orderDetail.getProduct().getProductID(), quantity, Integer::sum);
//        }
//        prescriptionOrderRepo.findByOrder_OrderID(orderId).ifPresent(prescriptionOrder -> {
//            if (prescriptionOrder.getPrescriptionOrderDetails() == null || prescriptionOrder.getPrescriptionOrderDetails().isEmpty()) {
//                return;
//            }
//            for (PrescriptionOrderDetail detail : prescriptionOrder.getPrescriptionOrderDetails()) {
//                if (detail == null) {
//                    continue;
//                }
//                Product frameProduct = detail.getFrame() == null ? null : detail.getFrame().getProduct();
//                Product lensProduct = detail.getLens() == null ? null : detail.getLens().getProduct();
//                addQuantity(qtyByProductId, frameProduct, 1);
//                addQuantity(qtyByProductId, lensProduct, 1);
//            }
//        });
//        return qtyByProductId;
//    }
//
//    private void addQuantity(Map<Long, Integer> qtyByProductId, Product product, int qty) {
//        if (product == null || product.getProductID() == null || qty <= 0) {
//            return;
//        }
//        qtyByProductId.merge(product.getProductID(), qty, Integer::sum);
//    }
}
