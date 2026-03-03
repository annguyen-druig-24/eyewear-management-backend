package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.config.FrontendProperties;
import com.swp391.eyewear_management_backend.config.vnpay.VnpayProperties;
import com.swp391.eyewear_management_backend.integration.vnpay.VnpayUtils;
import com.swp391.eyewear_management_backend.repository.PaymentRepo;
import com.swp391.eyewear_management_backend.service.VnpayCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments/vnpay")
@RequiredArgsConstructor
public class VnpayController {

    private final VnpayProperties vnpayProps;
    private final FrontendProperties feProps;
    private final VnpayCallbackService callbackService;
    private final PaymentRepo paymentRepo;

    /**
     * Return URL (browser redirect from VNPAY)
     * => BE verify + update DB + redirect về FE success page
     */
    @GetMapping("/return")
    public ResponseEntity<Void> vnpayReturn(HttpServletRequest request) {
        Map<String, String> vnpParams = extractVnpParams(request.getParameterMap());

        boolean valid = verifySignature(vnpParams);

        String rspCode = vnpParams.get("vnp_ResponseCode");   // "00" = success
        Long paymentId = parseLong(vnpParams.get("vnp_TxnRef"));
        long amount = parseLong0(vnpParams.get("vnp_Amount")); // amount * 100

        // 1) chữ ký sai / thiếu paymentId => redirect fail
        if (!valid || paymentId == null) {
            String failUrl = UriComponentsBuilder
                    .fromUriString(feProps.getBaseUrl() + feProps.getFailPath())
                    .queryParam("status", "INVALID_SIGNATURE")
                    .queryParam("code", rspCode)
                    .build()
                    .toUriString();

            return ResponseEntity.status(302).header("Location", failUrl).build();
        }

        // 2) cập nhật DB (tạm thời chốt theo return; IPN là chuẩn nhất sau này)
        VnpayCallbackService.IpResult ipResult = callbackService.handleCallback(paymentId, amount, rspCode);

        // 3) lấy orderId chắc chắn từ DB theo paymentId
        Long orderId = paymentRepo.findById(paymentId)
                .map(p -> (p.getOrder() != null ? p.getOrder().getOrderID() : null))
                .orElse(null);

        // 4) build redirect url về FE
        boolean ok = "00".equals(rspCode) && ipResult == VnpayCallbackService.IpResult.CONFIRM_SUCCESS;
        String status = ok ? "SUCCESS" : "FAILED";

        UriComponentsBuilder b = UriComponentsBuilder
                .fromUriString(feProps.getBaseUrl() + feProps.getSuccessPath())
                .queryParam("status", status)
                .queryParam("paymentId", paymentId)
                .queryParam("code", rspCode);

        // chỉ add orderId nếu có, tránh URL kiểu "...&orderId&..."
        if (orderId != null) {
            b.queryParam("orderId", orderId);
        }

        String redirectUrl = b.build().toUriString();
        return ResponseEntity.status(302).header("Location", redirectUrl).build();
    }

    /**
     * IPN URL (server-to-server)
     * VNPAY gọi vào đây để confirm chính thức (production nên dùng).
     */
    @GetMapping(value = "/ipn", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> vnpayIpn(HttpServletRequest request) {
        Map<String, String> vnpParams = extractVnpParams(request.getParameterMap());

        if (vnpParams.isEmpty()) {
            return Map.of("RspCode", "99", "Message", "Input data required");
        }

        if (!verifySignature(vnpParams)) {
            return Map.of("RspCode", "97", "Message", "Invalid signature");
        }

        Long paymentId = parseLong(vnpParams.get("vnp_TxnRef"));
        long amount = parseLong0(vnpParams.get("vnp_Amount"));
        String rspCode = vnpParams.get("vnp_ResponseCode");

        if (paymentId == null) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        var result = callbackService.handleCallback(paymentId, amount, rspCode);

        return switch (result) {
            case CONFIRM_SUCCESS, CONFIRM_FAILED -> Map.of("RspCode", "00", "Message", "Confirm Success");
            case ALREADY_CONFIRMED -> Map.of("RspCode", "02", "Message", "Order already confirmed");
            case INVALID_AMOUNT -> Map.of("RspCode", "04", "Message", "Invalid amount");
            case ORDER_NOT_FOUND -> Map.of("RspCode", "01", "Message", "Order not found");
        };
    }

    private Map<String, String> extractVnpParams(Map<String, String[]> raw) {
        Map<String, String> m = new HashMap<>();
        for (var e : raw.entrySet()) {
            String k = e.getKey();
            if (k != null && k.startsWith("vnp_")) {
                String[] v = e.getValue();
                if (v != null && v.length > 0) m.put(k, v[0]);
            }
        }
        return m;
    }

    private boolean verifySignature(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) return false;

        // copy + remove hash fields (VNPAY yêu cầu remove trước khi hash)
        Map<String, String> copy = new HashMap<>(params);
        copy.remove("vnp_SecureHash");
        copy.remove("vnp_SecureHashType");

        // sort + build hash data + sign
        Map<String, String> sorted = VnpayUtils.sortParams(copy);

        // QUAN TRỌNG:
        // - nếu VnpayUtils của bạn dùng buildHashData() thì dùng buildHashData()
        // - nếu chỉ có buildQueryString() thì thay bằng buildQueryString()
        String hashData;
        try {
            hashData = VnpayUtils.buildHashData(sorted);
        } catch (Throwable t) {
            // fallback nếu project bạn chưa có buildHashData
            hashData = VnpayUtils.buildQueryString(sorted);
        }

        String signed = VnpayUtils.hmacSHA512(vnpayProps.getHashSecret(), hashData);
        return secureHash.equalsIgnoreCase(signed);
    }

    private Long parseLong(String s) {
        try { return s == null ? null : Long.parseLong(s); }
        catch (Exception e) { return null; }
    }

    private long parseLong0(String s) {
        try { return s == null ? 0 : Long.parseLong(s); }
        catch (Exception e) { return 0; }
    }
}