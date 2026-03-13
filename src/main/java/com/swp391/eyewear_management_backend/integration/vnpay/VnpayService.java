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

@Service
@RequiredArgsConstructor
public class VnpayService {
    private static final DateTimeFormatter VNP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_TZ = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VnpayProperties props;
    private final FrontendProperties feProps;

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

    private String buildVnpayReturnUrl() {
        String base = feProps.getBaseUrl();
        String configured = props.getReturnUrl();

        String path = normalizeReturnPath(configured);
        if (base == null || base.isBlank()) {
            return path;
        }
        return joinUrl(base, path);
    }

    private String normalizeReturnPath(String configured) {
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
