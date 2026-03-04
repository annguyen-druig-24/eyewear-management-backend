package com.swp391.eyewear_management_backend.integration.vnpay;

import com.swp391.eyewear_management_backend.config.vnpay.VnpayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VnpayService {

    private final VnpayProperties props;

    private static final DateTimeFormatter VNP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_TZ = ZoneId.of("Asia/Ho_Chi_Minh");

    public String createVnpayPaymentUrl(Long orderId, Long paymentId, BigDecimal amount) {
        // VNPAY amount = VND * 100 (integer)  :contentReference[oaicite:3]{index=3}
        BigDecimal vnd = (amount == null ? BigDecimal.ZERO : amount).setScale(0, RoundingMode.HALF_UP); //Làm tròn số tiền
        long vnpAmount = vnd.multiply(new BigDecimal("100")).longValue(); //VNPAY ko chấp nhận dấu phẩy nên * 100

        String txnRef = String.valueOf(paymentId); // không trùng trong ngày :contentReference[oaicite:4]{index=4}
        String orderInfo = sanitizeOrderInfo("Thanh toan don hang " + orderId + " payment " + paymentId);

        ZonedDateTime now = ZonedDateTime.now(VN_TZ);
        String createDate = now.format(VNP_TIME);                                           //Thời điểm tạo giao dịch
        String expireDate = now.plusMinutes(props.getExpireMinutes()).format(VNP_TIME);     //Thời gian hết hạn link thanh toán = 15p

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
        params.put("vnp_ReturnUrl", props.getReturnUrl());
        params.put("vnp_IpAddr", getClientIpV4());
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        // KHÔNG gửi vnp_IpnUrl trong params (hay gây gateway reject).
        // IPN URL hãy cấu hình trong portal VNPAY sandbox + dùng ngrok khi dev local.

        // sort + sign
        var sorted = VnpayUtils.sortParams(params);
        String hashData = VnpayUtils.buildHashData(sorted);
        String secureHash = VnpayUtils.hmacSHA512(props.getHashSecret(), hashData);

        sorted.put("vnp_SecureHash", secureHash);
        return VnpayUtils.buildRedirectUrl(props.getPayUrl(), sorted);
    }

    private String getClientIpV4() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "127.0.0.1";

            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            String ip = (xff != null && !xff.isBlank())
                    ? xff.split(",")[0].trim()
                    : req.getRemoteAddr();

            if (ip == null || ip.isBlank()) return "127.0.0.1";

            // IPv6 loopback -> IPv4 loopback
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) return "127.0.0.1";

            // Nếu vẫn là IPv6, tạm fallback IPv4 để tránh gateway kén
            if (ip.contains(":")) return "127.0.0.1";

            return ip;
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    /**
     * VNPAY yêu cầu OrderInfo không dấu, không ký tự đặc biệt :contentReference[oaicite:5]{index=5}
     * -> ta normalize + chỉ giữ chữ/số/space + một ít dấu an toàn (.,:)
     */
    private String sanitizeOrderInfo(String input) {
        if (input == null) return "Thanh toan don hang";
        String s = input.trim();

        // remove accents
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // keep safe chars: letters, digits, space, dot, comma, colon
        s = s.replaceAll("[^A-Za-z0-9 ., :_-]", " ");

        // collapse spaces
        s = s.replaceAll("\\s+", " ").trim();

        // limit length 255
        if (s.length() > 255) s = s.substring(0, 255);

        return s.isEmpty() ? "Thanh toan don hang" : s;
    }
}