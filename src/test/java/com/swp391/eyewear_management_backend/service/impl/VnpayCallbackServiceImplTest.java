package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.config.OrderConstants;
import com.swp391.eyewear_management_backend.entity.Invoice;
import com.swp391.eyewear_management_backend.entity.Order;
import com.swp391.eyewear_management_backend.entity.Payment;
import com.swp391.eyewear_management_backend.entity.ShippingInfo;
import com.swp391.eyewear_management_backend.repository.InvoiceRepo;
import com.swp391.eyewear_management_backend.repository.OrderRepo;
import com.swp391.eyewear_management_backend.repository.PaymentRepo;
import com.swp391.eyewear_management_backend.repository.ShippingInfoRepo;
import com.swp391.eyewear_management_backend.service.VnpayCallbackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VnpayCallbackServiceImplTest {

    @Mock
    PaymentRepo paymentRepo;

    @Mock
    OrderRepo orderRepo;

    @Mock
    InvoiceRepo invoiceRepo;

    @Mock
    ShippingInfoRepo shippingInfoRepo;

    @Mock
    CheckoutCartTrackingService checkoutCartTrackingService;

    @Mock
    OrderEmailNotificationService orderEmailNotificationService;

    @InjectMocks
    VnpayCallbackServiceImpl service;

    @Test
    void handleCallback_whenFail_shouldCancelOrderAndCancelRemainingPayment() {
        Order order = Order.builder()
                .orderID(1L)
                .orderStatus(OrderConstants.ORDER_STATUS_PENDING)
                .build();

        ShippingInfo shippingInfo = new ShippingInfo();
        shippingInfo.setOrder(order);
        order.setShippingInfo(shippingInfo);

        Payment depositPayment = Payment.builder()
                .paymentID(10L)
                .order(order)
                .paymentPurpose(OrderConstants.PAYMENT_PURPOSE_DEPOSIT)
                .paymentMethod("VNPAY")
                .amount(new BigDecimal("1300000.00"))
                .status(OrderConstants.PAYMENT_STATUS_PENDING)
                .build();

        Payment remainingPayment = Payment.builder()
                .paymentID(11L)
                .order(order)
                .paymentPurpose(OrderConstants.PAYMENT_PURPOSE_REMAINING)
                .paymentMethod(OrderConstants.PAYMENT_METHOD_COD)
                .amount(new BigDecimal("540900.00"))
                .status(OrderConstants.PAYMENT_STATUS_PENDING)
                .build();

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setStatus(OrderConstants.INVOICE_STATUS_UNPAID);

        when(paymentRepo.findByIdForUpdate(10L)).thenReturn(Optional.of(depositPayment));
        when(invoiceRepo.findByOrderOrderID(1L)).thenReturn(Optional.of(invoice));
        when(paymentRepo.findByOrderIdAndPurposeAndStatusForUpdate(
                eq(1L),
                eq(OrderConstants.PAYMENT_PURPOSE_REMAINING),
                eq(OrderConstants.PAYMENT_STATUS_PENDING)
        )).thenReturn(List.of(remainingPayment));

        long vnpAmount = 1300000L * 100;
        VnpayCallbackService.IpResult result = service.handleCallback(10L, vnpAmount, "24", "02");

        assertThat(result).isEqualTo(VnpayCallbackService.IpResult.CONFIRM_FAILED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderConstants.ORDER_STATUS_CANCELED);
        assertThat(shippingInfo.getShippingStatus()).isEqualTo(OrderConstants.SHIPPING_STATUS_CANCELED);
        assertThat(invoice.getStatus()).isEqualTo(OrderConstants.INVOICE_STATUS_CANCELED);
        assertThat(depositPayment.getStatus()).isEqualTo(OrderConstants.PAYMENT_STATUS_FAILED);
        assertThat(remainingPayment.getStatus()).isEqualTo(OrderConstants.PAYMENT_STATUS_CANCELED);
        assertThat(remainingPayment.getPaymentDate()).isNotNull();

        verify(paymentRepo).save(depositPayment);
        verify(orderRepo).save(order);
        verify(shippingInfoRepo).save(shippingInfo);
        verify(invoiceRepo).save(invoice);
        verify(orderEmailNotificationService, never()).sendOrderSuccessEmailSafely(any(), any());

        ArgumentCaptor<List<Payment>> captor = ArgumentCaptor.forClass(List.class);
        verify(paymentRepo).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getPaymentID()).isEqualTo(11L);
    }

    @Test
    void handleCallback_whenSuccess_shouldSendOrderSuccessEmail() {
        Order order = Order.builder()
                .orderID(2L)
                .orderCode("ORD-TEST-ONLINE")
                .orderStatus(OrderConstants.ORDER_STATUS_PENDING)
                .build();

        ShippingInfo shippingInfo = new ShippingInfo();
        shippingInfo.setOrder(order);
        shippingInfo.setRecipientEmail("customer@example.com");
        shippingInfo.setRecipientName("Customer Test");
        order.setShippingInfo(shippingInfo);

        Payment fullPayment = Payment.builder()
                .paymentID(20L)
                .order(order)
                .paymentPurpose(OrderConstants.PAYMENT_PURPOSE_FULL)
                .paymentMethod("VNPAY")
                .amount(new BigDecimal("1540900.00"))
                .status(OrderConstants.PAYMENT_STATUS_PENDING)
                .build();

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setStatus(OrderConstants.INVOICE_STATUS_UNPAID);

        when(paymentRepo.findByIdForUpdate(20L)).thenReturn(Optional.of(fullPayment));
        when(invoiceRepo.findByOrderOrderID(2L)).thenReturn(Optional.of(invoice));

        long vnpAmount = 1540900L * 100;
        VnpayCallbackService.IpResult result = service.handleCallback(20L, vnpAmount, "00", "00");

        assertThat(result).isEqualTo(VnpayCallbackService.IpResult.CONFIRM_SUCCESS);
        assertThat(order.getOrderStatus()).isEqualTo(OrderConstants.ORDER_STATUS_PAID);
        assertThat(invoice.getStatus()).isEqualTo(OrderConstants.INVOICE_STATUS_PAID);
        assertThat(fullPayment.getStatus()).isEqualTo(OrderConstants.PAYMENT_STATUS_SUCCESS);

        verify(paymentRepo).save(fullPayment);
        verify(orderRepo).save(order);
        verify(invoiceRepo).save(invoice);
        verify(checkoutCartTrackingService).cleanupTrackedCartItems(order);
        verify(orderEmailNotificationService).sendOrderSuccessEmailSafely(order, fullPayment);
    }
}
