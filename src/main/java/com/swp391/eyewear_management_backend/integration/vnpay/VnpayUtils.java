package com.swp391.eyewear_management_backend.integration.vnpay;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/*
    - Utility: sort params, build hashData/queryString, ký HMAC SHA512, build redirect URL.
*/

public final class VnpayUtils {
    private VnpayUtils() {}

    /*
        1) Mục đích: ký chuỗi hashData theo chuẩn VNPAY.
        2) Sử dụng ở đâu:
            - tạo URL thanh toán (`VnpayService`),
            - verify callback (`VnpayController`).
        3) Steps:
              1. Khởi tạo `Mac` algorithm `HmacSHA512`.
              2. Init key từ `hashSecret`.
              3. `doFinal(data)` để lấy bytes chữ ký.
              4. Convert bytes -> hex lower-case.
        4) Ý nghĩa logic: đảm bảo toàn vẹn dữ liệu query giữa BE và VNPAY.
    */
    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey =
                    new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hash = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) hash.append(String.format("%02x", b));
            return hash.toString();
        } catch (Exception e) {
            throw new RuntimeException("VNPAY HMAC error", e);
        }
    }

    /** Sort theo key a-z (querystring) */
    /*
        1) Mục đích: sắp xếp params theo key tăng dần
        2) Sử dụng ở đâu: trước khi build hashData/queryString.
        3) Steps:
              1. Duyệt params đầu vào.
              2. Bỏ key null, value null/blank.
              3. Đưa vào `TreeMap` để auto sort.
        4) Logic: VNPAY yêu cầu thứ tự ổn định khi ký.
    */
    public static Map<String, String> sortParams(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>();
        for (var e : params.entrySet()) {
            if (e.getKey() == null) continue;
            String v = e.getValue();
            if (v != null && !v.isBlank()) {
                sorted.put(e.getKey(), v);
            }
        }
        return sorted;
    }

    /**
     * Build "hashData" đúng theo Java sample của VNPAY:
     * - key KHÔNG encode
     * - value encode US_ASCII
     */
    /*
        1) Mục đích: tạo chuỗi dùng để ký (`key=value&...`) theo quy tắc VNPAY.
        2) Sử dụng ở đâu: tạo signature khi request/verify.
        3) Steps:
              1. Duyệt map đã sort.
              2. Key giữ nguyên, value encode ASCII.
              3. Nối `&` giữa các cặp.
        4) Logic: hashData phải đúng format thì chữ ký mới khớp.
    */
    public static String buildHashData(Map<String, String> sortedParams) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (var e : sortedParams.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            if (val == null || val.isBlank()) continue;

            if (!first) sb.append("&");
            first = false;

            sb.append(key);
            sb.append("=");
            sb.append(urlEncodeAscii(val));
        }
        return sb.toString();
    }

    /** Build query string để redirect: encode cả key và value (US_ASCII) */
    /*
        - Mục đích: tạo query string redirect thật sự.
        - Khác `buildHashData`: encode cả key và value.
    */
    public static String buildQueryString(Map<String, String> sortedParams) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (var e : sortedParams.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            if (val == null || val.isBlank()) continue;

            if (!first) sb.append("&");
            first = false;

            sb.append(urlEncodeAscii(key));
            sb.append("=");
            sb.append(urlEncodeAscii(val));
        }
        return sb.toString();
    }


    //Mục đích: ghép `baseUrl + ? + queryString`.
    public static String buildRedirectUrl(String baseUrl, Map<String, String> sortedParams) {
        return baseUrl + "?" + buildQueryString(sortedParams);
    }

    //Mục đích: URL encode bằng US_ASCII; fallback UTF-8 nếu lỗi.
    private static String urlEncodeAscii(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.US_ASCII.toString());
        } catch (Exception e) {
            // fallback
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        }
    }
}