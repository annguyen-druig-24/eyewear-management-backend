package com.swp391.eyewear_management_backend.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swp391.eyewear_management_backend.dto.request.PaymentRequest;
import com.swp391.eyewear_management_backend.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse createPaymentLink(PaymentRequest request);

    String checkOrderStatus(Long orderCode);

    void processWebhook(ObjectNode webhookBody);

    String createPayOSPaymentUrl(Long paymentId, long amount, String orderCodeStr);
}