package com.swp391.eyewear_management_backend.integration.vnpay;

import com.swp391.eyewear_management_backend.config.FrontendProperties;
import com.swp391.eyewear_management_backend.config.vnpay.VnpayProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/*
    - Service chính tạo URL redirect sang VNPAY sandbox.
          - chuẩn hóa amount về integer VND,
          - nhân 100 theo chuẩn VNPAY,
          - set params chuẩn (`vnp_Version`, `vnp_Command`, ...),
          - sort params,
          - tạo `hashData`,
          - ký HMAC SHA512 bằng `hashSecret`,
          - append `vnp_SecureHash`,
          - trả URL sandbox hoàn chỉnh.
*/

@Service
@RequiredArgsConstructor
public class VnpayService {
    private static final DateTimeFormatter VNP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_TZ = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VnpayProperties props;
    private final FrontendProperties feProps;

    /*
        1) Mục đích: sinh URL thanh toán VNPAY sandbox hoàn chỉnh.
        2) Được dùng ở đâu: `PaymentGatewayServiceImpl#createPaymentUrl` khi method=VNPAY.
        3) Steps:
              1. Chuẩn hóa `amount` về số nguyên VND (`setScale(0)`).
              2. Tính `vnp_Amount = amount * 100` (quy ước VNPAY).
              3. Tạo `txnRef = paymentId` (mapping callback ngược về DB).
              4. Tạo `orderInfo` và sanitize để không lỗi ký tự.
              5. Tạo `createDate`, `expireDate` theo múi giờ VN.
              6. Put đủ params chuẩn vào map.
              7. Sort params.
              8. Build hashData + ký HMAC SHA512.
              9. Add `vnp_SecureHash`.
              10. Trả redirect URL.
         4) Logic: bảo đảm params đúng chuẩn VNPAY, signed đúng secret, có `txnRef` để đối soát callback.
    */
    public String createVnpayPaymentUrl(Long orderId, Long paymentId, BigDecimal amount) {
        BigDecimal vnd = (amount == null ? BigDecimal.ZERO : amount).setScale(0, RoundingMode.HALF_UP);
        long vnpAmount = vnd.multiply(new BigDecimal("100")).longValue();

        String txnRef = String.valueOf(paymentId);
        String orderInfo = sanitizeOrderInfo("Thanh toan don hang " + orderId + " payment " + paymentId);

        ZonedDateTime now = ZonedDateTime.now(VN_TZ);
        String createDate = now.format(VNP_TIME);
        String expireDate = now.plusMinutes(props.getExpireMinutes()).format(VNP_TIME);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", props.getVersion());
        params.put("vnp_Command", props.getCommand());
        params.put("vnp_TmnCode", props.getTmnCode());
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", props.getCurrCode());
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", props.getOrderType());
        params.put("vnp_Locale", props.getLocale());
        params.put("vnp_ReturnUrl", buildVnpayReturnUrl());
        params.put("vnp_IpAddr", getClientIpV4());
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        Map<String, String> sorted = VnpayUtils.sortParams(params);
        String hashData = VnpayUtils.buildHashData(sorted);
        String secureHash = VnpayUtils.hmacSHA512(props.getHashSecret(), hashData);
        sorted.put("vnp_SecureHash", secureHash);

        return VnpayUtils.buildRedirectUrl(props.getPayUrl(), sorted);
    }

    /*
        1) Mục đích: xác định URL return sẽ gửi cho VNPAY.
        2) Logic:
            - Nếu cấu hình return-url là URL public hợp lệ và **không phải localhost backend** => dùng luôn.
            - Nếu localhost/misconfig => fallback dùng FE base URL + normalized path.
    */
    private String buildVnpayReturnUrl() {
        String configured = props.getReturnUrl();
        if (configured != null && (configured.startsWith("http://") || configured.startsWith("https://"))) {
            try {
                URL url = new URL(configured);
                String host = url.getHost();
                int port = url.getPort();

                boolean isLocalBackend = ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host))
                        && (port == -1 || port == 8080);

                if (!isLocalBackend) {
                    return configured;
                }
            } catch (Exception ignored) {
            }
        }

        String base = feProps.getBaseUrl();
        String path = normalizeFrontendReturnPath(configured);
        if (base == null || base.isBlank()) return path;
        return joinUrl(base, path);
    }

    /*
        1) Mục đích: chuẩn hóa path return cho FE.
    */
    private String normalizeFrontendReturnPath(String configured) {
        String path = configured;
        if (path == null || path.isBlank()) {
            path = "/payment/vnpay/return";
        } else if (path.startsWith("http://") || path.startsWith("https://")) {
            try {
                URL url = new URL(path);
                String p = url.getPath();
                path = (p == null || p.isBlank()) ? "/payment/vnpay/return" : p;
            } catch (Exception ignored) {
                path = "/payment/vnpay/return";
            }
        }

        if ("/payments/vnpay/return".equals(path)) {
            path = "/payment/vnpay/return";
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    // - Mục đích: join URL tránh lỗi `//` hoặc thiếu `/`.
    private String joinUrl(String base, String path) {
        String b = base.trim();
        String p = (path == null ? "" : path.trim());

        if (b.endsWith("/") && p.startsWith("/")) {
            return b.substring(0, b.length() - 1) + p;
        }
        if (!b.endsWith("/") && !p.startsWith("/")) {
            return b + "/" + p;
        }
        return b + p;
    }

    /*
        1) Mục đích: lấy IPv4 client để gửi `vnp_IpAddr`.
        2) ưu tiên `X-Forwarded-For`, fallback `remoteAddr`, normalize localhost/IPv6 về `127.0.0.1`.
    */
    private String getClientIpV4() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "127.0.0.1";

            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            String ip = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();

            if (ip == null || ip.isBlank()) return "127.0.0.1";
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) return "127.0.0.1";
            if (ip.contains(":")) return "127.0.0.1";

            return ip;
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    /*
        1) Mục đích: làm sạch chuỗi order info.
        2) Steps: bỏ dấu tiếng Việt, remove ký tự lạ, rút gọn whitespace, cắt max 255.
    */
    private String sanitizeOrderInfo(String input) {
        if (input == null) return "Thanh toan don hang";
        String s = input.trim();

        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        s = s.replaceAll("[^A-Za-z0-9 ., :_-]", " ");
        s = s.replaceAll("\\s+", " ").trim();

        if (s.length() > 255) s = s.substring(0, 255);
        return s.isEmpty() ? "Thanh toan don hang" : s;
    }
}
