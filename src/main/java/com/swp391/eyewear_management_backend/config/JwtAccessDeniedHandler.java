package com.swp391.eyewear_management_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.eyewear_management_backend.dto.response.ApiResponse;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/*
    * Class này dùng để chặn quyền khi gọi method nhưng chỉ chặn quyền khi sử hasAnyAuthority(String ...) chứ ko dùng hasAnyRole(String ...)
    * Trả JSON lỗi chuẩn khi có token nhưng không đủ quyền (403).
 */
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    /*
        1) Dùng để làm gì? --> Trả response chuẩn khi user đã xác thực nhưng thiếu quyền (403)
        2) Được dùng ở đâu? --> Khi `@PreAuthorize` hoặc `hasAnyAuthority` không thỏa
        3) Steps:
            1. Lấy `ErrorCode.UNAUTHORIZED`.
            2. Set status/content type.
            3. Trả JSON lỗi thống nhất.
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        response.setStatus(errorCode.getHttpStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));

        response.flushBuffer();
    }
}
