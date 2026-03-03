package com.swp391.eyewear_management_backend.integration.vnpay;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public final class VnpayUtils {
    private VnpayUtils() {}

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

    public static String buildRedirectUrl(String baseUrl, Map<String, String> sortedParams) {
        return baseUrl + "?" + buildQueryString(sortedParams);
    }

    private static String urlEncodeAscii(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.US_ASCII.toString());
        } catch (Exception e) {
            // fallback
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        }
    }
}