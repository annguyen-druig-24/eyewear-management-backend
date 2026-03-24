package com.swp391.eyewear_management_backend.service;

public interface EmailService {
    void sendOrderSuccessEmail(String toEmail, String customerName, String orderId, double totalAmount);
    void sendOrderCancelEmail(String toEmail, String customerName, String orderId);
    void sendOrderRefundEmail(String toEmail, String customerName, String orderId, double refundAmount);
}