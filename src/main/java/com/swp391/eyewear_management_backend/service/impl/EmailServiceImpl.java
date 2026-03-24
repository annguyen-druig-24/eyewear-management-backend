package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private TemplateEngine templateEngine;

    // Hàm chung để gửi mail HTML (Hàm này dùng nội bộ nên dùng private)
    private void sendHtmlEmail(String to, String subject, String templateName, Context context) {
        try {
            String htmlContent = templateEngine.process(templateName, context);
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = gửi dưới dạng HTML

            javaMailSender.send(message);
            System.out.println("Đã gửi mail (" + subject + ") đến: " + to);
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi email: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void sendOrderSuccessEmail(String toEmail, String customerName, String orderId, double totalAmount) {
        Context context = new Context();
        context.setVariable("customerName", customerName);
        context.setVariable("orderId", orderId);
        context.setVariable("totalAmount", String.format("%,.0f đ", totalAmount));
        context.setVariable("message", "Đơn hàng của bạn đã được ghi nhận thành công và đang được xử lý.");

        sendHtmlEmail(toEmail, "Đặt hàng thành công tại Eyewear Sora!", "order-mail-template", context);
    }

    @Override
    @Async
    public void sendOrderCancelEmail(String toEmail, String customerName, String orderId) {
        Context context = new Context();
        context.setVariable("customerName", customerName);
        context.setVariable("orderId", orderId);
        context.setVariable("totalAmount", "Đã hủy");
        context.setVariable("message", "Rất tiếc, đơn hàng của bạn đã bị hủy. Nếu có thắc mắc, vui lòng liên hệ CSKH.");

        sendHtmlEmail(toEmail, "Thông báo hủy đơn hàng #" + orderId, "order-mail-template", context);
    }

    @Override
    @Async
    public void sendOrderRefundEmail(String toEmail, String customerName, String orderId, double refundAmount) {
        Context context = new Context();
        context.setVariable("customerName", customerName);
        context.setVariable("orderId", orderId);
        context.setVariable("totalAmount", String.format("%,.0f đ", refundAmount));
        context.setVariable("message", "Shop đã hoàn tất việc hoàn tiền cho đơn hàng của bạn. Tiền sẽ về tài khoản trong 1-3 ngày làm việc.");

        sendHtmlEmail(toEmail, "Thông báo hoàn tiền đơn hàng #" + orderId, "order-mail-template", context);
    }
}