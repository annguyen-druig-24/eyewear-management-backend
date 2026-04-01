package com.swp391.eyewear_management_backend.controller;

import com.nimbusds.jose.JOSEException;
import com.swp391.eyewear_management_backend.dto.request.AuthenticationRequest;
import com.swp391.eyewear_management_backend.dto.request.IntrospectRequest;
import com.swp391.eyewear_management_backend.dto.request.LogoutRequest;
import com.swp391.eyewear_management_backend.dto.request.RefreshRequest;
import com.swp391.eyewear_management_backend.dto.response.ApiResponse;
import com.swp391.eyewear_management_backend.dto.response.AuthenticationResponse;
import com.swp391.eyewear_management_backend.dto.response.IntrospectResponse;
import com.swp391.eyewear_management_backend.service.impl.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

/*
    - API auth: `/auth/token`, `/auth/introspect`, `/auth/logout`, `/auth/refresh`.
*/

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;

    /*
        - Nhận username/password, gọi `AuthenticationService.authenticate()`.
        - Trả token + thông tin role/name.
     */
    @PostMapping("/token")
    ApiResponse<AuthenticationResponse> logIn(@RequestBody AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    /*
        - Kiểm tra tính hợp lệ của token (không nhất thiết cấp quyền).
     */
    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> logIn(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    /*
        - Vô hiệu hóa token hiện tại (blacklist theo `jti`).
     */
    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder()
                .build();
    }

    /*
        - Verify refresh window, blacklist token cũ, cấp token mới.
     */
    @PostMapping("/refresh")
    ApiResponse<AuthenticationResponse> refresh(@RequestBody RefreshRequest request) throws ParseException, JOSEException {
        var result = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }
}
