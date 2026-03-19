package com.swp391.eyewear_management_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swp391.eyewear_management_backend.config.FrontendProperties;
import com.swp391.eyewear_management_backend.dto.request.PaymentRequest;
import com.swp391.eyewear_management_backend.dto.response.PaymentResponse;
import com.swp391.eyewear_management_backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final FrontendProperties frontendProperties;

    @PostMapping("/create-payos")
    public ResponseEntity<?> createPaymentLink(@RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = paymentService.createPaymentLink(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi tạo thanh toán: ", e); // Dùng log thay cho e.printStackTrace()
            return ResponseEntity.internalServerError().body("Lỗi tạo thanh toán: " + e.getMessage());
        }
    }

    @GetMapping("/check-status/{orderCode}")
    public ResponseEntity<?> checkStatus(@PathVariable Long orderCode) {
        String status = paymentService.checkOrderStatus(orderCode);
        Map<String, String> response = new HashMap<>();
        response.put("status", status);
        return ResponseEntity.ok(response);
    }

    // API 3: Dành riêng cho Ngrok và PayOS đâm vào
    @PostMapping("/payos-webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody String jsonBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode webhookBody = mapper.readValue(jsonBody, ObjectNode.class);

            // Giao cho Service xử lý như bình thường
            paymentService.processWebhook(webhookBody);

            Map<String, Boolean> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi xử lý webhook: ", e); // Dùng log thay cho e.printStackTrace()
            Map<String, Boolean> response = new HashMap<>();
            response.put("success", false);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/payos-return")
    public ResponseEntity<Void> handlePayosReturn() {
        return ResponseEntity.status(302)
                .header("Location", buildFrontendUrl(frontendProperties.getSuccessPath()))
                .build();
    }

    @GetMapping("/payos-cancel")
    public ResponseEntity<Void> handlePayosCancel() {
        return ResponseEntity.status(302)
                .header("Location", buildFrontendUrl(frontendProperties.getCancelPath()))
                .build();
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
