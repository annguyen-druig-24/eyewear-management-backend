package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestEmailController {
    @Autowired
    private EmailService emailService;

    @GetMapping("/api/test-email")
    public String testEmail(@RequestParam String email) {
        // Dùng dữ liệu giả để test thử hàm Gửi mail thành công
        emailService.sendOrderSuccessEmail(
                email,
                "Khách Hàng VIP",
                "ORD-9999",
                1550000
        );
        return "Đã ra lệnh bắn mail tới: " + email + ". Chờ 2-3 giây rồi check hộp thư nha!";
    }
}
