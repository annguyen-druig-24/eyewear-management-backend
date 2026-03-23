package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.request.ProductTryOnConfigUploadRequest;
import com.swp391.eyewear_management_backend.dto.response.ApiResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductTryOnConfigResponse;
import com.swp391.eyewear_management_backend.service.ProductTryOnConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/{productId}/try-on-config")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductTryOnConfigController {

    private final ProductTryOnConfigService productTryOnConfigService;

    @GetMapping
    public ApiResponse<ProductTryOnConfigResponse> getTryOnConfig(@PathVariable Long productId) {
        return ApiResponse.<ProductTryOnConfigResponse>builder()
                .message("Get try-on config successfully")
                .result(productTryOnConfigService.getByProductId(productId))
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ApiResponse<ProductTryOnConfigResponse> uploadTryOnModel(
            @PathVariable Long productId,
            @ModelAttribute ProductTryOnConfigUploadRequest request,
            @RequestPart("modelFile") MultipartFile modelFile) throws IOException {
        return ApiResponse.<ProductTryOnConfigResponse>builder()
                .message("Upload try-on model successfully")
                .result(productTryOnConfigService.uploadTryOnModel(productId, request, modelFile))
                .build();
    }

    @DeleteMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteTryOnConfig(@PathVariable Long productId) throws IOException {
        productTryOnConfigService.deleteByProductId(productId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Delete try-on config successfully")
                .build());
    }
}