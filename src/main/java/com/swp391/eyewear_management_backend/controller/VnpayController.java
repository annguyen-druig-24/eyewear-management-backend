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

/*
    - Expose endpoint `/payments/vnpay/return` và `/payments/vnpay/ipn`.
    - Verify signature, parse dữ liệu callback, gọi service xử lý kết quả.
*/

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
    /*
        1) Mục đích: xử lý callback return qua browser và redirect người dùng về FE.
        2) Dùng ở đâu: endpoint `GET /payments/vnpay/return`.
        3) Steps:
              1. Trích params `vnp_*` bằng `extractVnpParams`.
              2. Verify signature (`verifySignature`).
              3. Parse `rspCode`, `txnStatus`, `paymentId`, `amount`.
              4. Nếu signature lỗi hoặc thiếu paymentId -> redirect fail page FE.
              5. Gọi callback service cập nhật DB.
              6. Quyết định targetPath:
                 - success khi `rspCode=00` + `txnStatus=00` + service confirm success,
                 - cancel khi mã cancel (`24` hoặc `02`),
                 - còn lại fail.
              7. Redirect 302 về FE.
         4) Logic: return endpoint ưu tiên UX redirect user; nghiệp vụ update DB vẫn thực hiện.
    */
    @GetMapping("/return")
    public ResponseEntity<Void> vnpayReturn(HttpServletRequest request) {
        Map<String, String> vnpParams = extractVnpParams(request.getParameterMap());

        boolean valid = verifySignature(vnpParams);

        //String rspCode = vnpParams.get("vnp_ResponseCode");   // mã trạng thái giao dịch "00" = success
        String rspCode = vnpParams.get("vnp_ResponseCode");   // mã phản hồi request (chỉ cho biết request hợp lệ)
        String txnStatus = vnpParams.get("vnp_TransactionStatus"); // mã trạng thái thanh toán thực tế
        Long paymentId = parseLong(vnpParams.get("vnp_TxnRef"));    //paymentId (id Payment trong DB)
        long amount = parseLong0(vnpParams.get("vnp_Amount")); // amount * 100

        // 1) chữ ký sai / thiếu paymentId => redirect fail
        if (!valid || paymentId == null) {
//            String failUrl = UriComponentsBuilder
//                    .fromUriString(feProps.getBaseUrl() + feProps.getFailPath())
//                    .queryParam("status", "INVALID_SIGNATURE")
//                    .queryParam("code", rspCode)
//                    .build()
//                    .toUriString();
//
//            return ResponseEntity.status(302).header("Location", failUrl).build();
            return ResponseEntity.status(302)
                    .header("Location", buildFrontendRedirectUrl(feProps.getFailPath()))
                    .build();
        }

        // 2) cập nhật DB (tạm thời chốt theo return; IPN là chuẩn nhất sau này)
        //VnpayCallbackService.IpResult ipResult = callbackService.handleCallback(paymentId, amount, rspCode);
        VnpayCallbackService.IpResult ipResult = callbackService.handleCallback(paymentId, amount, rspCode, txnStatus);

        // 3) lấy orderId chắc chắn từ DB theo paymentId
        // Mục tiêu: FE biết order nào vừa thanh toán
//        Long orderId = paymentRepo.findById(paymentId)
//                .map(p -> (p.getOrder() != null ? p.getOrder().getOrderID() : null))
//                .orElse(null);

        // 4) build redirect url về FE
        //boolean ok = "00".equals(rspCode) && ipResult == VnpayCallbackService.IpResult.CONFIRM_SUCCESS;
        boolean ok = "00".equals(rspCode) && "00".equals(txnStatus) && ipResult == VnpayCallbackService.IpResult.CONFIRM_SUCCESS;

        String targetPath;
        String status;

        if (ok) {
            targetPath = feProps.getSuccessPath();       // /success
            //status = "SUCCESS";
        } else if ("24".equals(rspCode) || "02".equals(txnStatus)) {
            targetPath = feProps.getCancelPath();        // /cancel
            //status = "CANCELLED";
        } else {
            targetPath = feProps.getFailPath();          // /payment-result
            //status = "FAILED";
        }

//        UriComponentsBuilder b = UriComponentsBuilder
//                .fromUriString(feProps.getBaseUrl() + targetPath)
//                .queryParam("status", status)
//                .queryParam("paymentId", paymentId)
//                //.queryParam("code", rspCode);
//                .queryParam("code", rspCode)
//                .queryParam("txnStatus", txnStatus);
//
//        if (orderId != null) {
//            b.queryParam("orderId", orderId);
//        }
//
//        String redirectUrl = b.build().toUriString();
//        return ResponseEntity.status(302).header("Location", redirectUrl).build();
        return ResponseEntity.status(302)
                .header("Location", buildFrontendRedirectUrl(targetPath))
                .build();
    }

    /**
     * IPN URL (server-to-server)
     * VNPAY gọi vào đây để confirm chính thức (production nên dùng).
     */
    /*
         1) Mục đích: xử lý IPN server-server chính thức từ VNPAY.
         2) Dùng ở đâu: endpoint `GET /payments/vnpay/ipn`.
         3) Steps:
              1. Validate input rỗng.
              2. Verify signature.
              3. Parse paymentId/amount/responseCode/transactionStatus.
              4. Gọi callback service.
              5. Map `IpResult` -> `RspCode/Message` chuẩn để trả VNPAY.
         4) Logic: đây là endpoint chuẩn đối soát chính xác trong production.

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
        String txnStatus = vnpParams.get("vnp_TransactionStatus");

        if (paymentId == null) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        //var result = callbackService.handleCallback(paymentId, amount, rspCode);
        var result = callbackService.handleCallback(paymentId, amount, rspCode, txnStatus);

        return switch (result) {
            case CONFIRM_SUCCESS, CONFIRM_FAILED -> Map.of("RspCode", "00", "Message", "Confirm Success");
            case ALREADY_CONFIRMED -> Map.of("RspCode", "02", "Message", "Order already confirmed");
            case INVALID_AMOUNT -> Map.of("RspCode", "04", "Message", "Invalid amount");
            case ORDER_NOT_FOUND -> Map.of("RspCode", "01", "Message", "Order not found");
        };
    }

    /*  Cách thức: duyệt từng param, check nếu key bắt đầu bằng vnp_ thì lấy giá trị đầu tiên v[0] --> trả về map mới chỉ chứa data VNPAY
         Mục tiêu: tránh bị nhiễu bởi các query param khác + chuẩn hóa dữ liệu để verify signature */
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

    /*
        1) Mục đích: xác thực callback đúng từ VNPAY.
        2) Steps:
              1. Lấy `vnp_SecureHash`.
              2. Copy map và remove `vnp_SecureHash`, `vnp_SecureHashType`.
              3. Sort params.
              4. Build `hashData`.
              5. Ký lại bằng `hashSecret`.
              6. So sánh ignore-case với hash nhận được.
         3) Logic: chặn giả mạo callback.
    */
    private boolean verifySignature(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");               // Lấy chữ ký từ VNPAY: vnp_SecureHash
        if (secureHash == null || secureHash.isBlank()) return false;   // Nếu ko có chữ ký --> fail

        // copy + remove hash fields (VNPAY yêu cầu remove trước khi hash)
        Map<String, String> copy = new HashMap<>(params);
        copy.remove("vnp_SecureHash");
        copy.remove("vnp_SecureHashType");

        // sort + build hash data + sign
        // Mục tiêu: VNPAY quy định thứ tự params khi hash phải ổn định --> thường hash theo kiểu từ nhỏ đến lớn
        Map<String, String> sorted = VnpayUtils.sortParams(copy);

        /* LÝ THUYẾT: hashData là chuỗi key=value&key=value... theo chuẩn VNPAY để đem đi ký
        QUAN TRỌNG:
         - nếu VnpayUtils của bạn dùng buildHashData() thì dùng buildHashData()
         - nếu chỉ có buildQueryString() thì thay bằng buildQueryString() */
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

    //Mục đích: build URL redirect FE an toàn slash.
    private String buildFrontendRedirectUrl(String path) {
        String baseUrl = feProps.getBaseUrl();
        String safePath = (path == null || path.isBlank()) ? "/" : path;

        if (baseUrl.endsWith("/") && safePath.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + safePath;
        }
        if (!baseUrl.endsWith("/") && !safePath.startsWith("/")) {
            return baseUrl + "/" + safePath;
        }
        return baseUrl + safePath;
    }
}