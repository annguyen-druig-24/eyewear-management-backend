package com.swp391.eyewear_management_backend.config;

import com.swp391.eyewear_management_backend.dto.response.ApiResponse;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.awt.*;
import java.io.IOException;

/*
    - Trả JSON lỗi chuẩn khi request chưa xác thực (401).
*/

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /*
        1) Dùng để làm gì? --> Trả response chuẩn khi chưa đăng nhập/ token sai (401).
        2) Được dùng ở đâu? --> Khi authentication fail trong filter chain
        3) Steps:
            1. Lấy `ErrorCode.UNAUTHENTICATED`.
            2. Set HTTP status.
            3. Trả JSON `ApiResponse` với `code` + `message`.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;

        response.setStatus(errorCode.getHttpStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        ObjectMapper objectMapper = new ObjectMapper(); //Dùng để convert apiResponse thành String vì nó đang là dạng object
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));

        response.flushBuffer();
    }
}
