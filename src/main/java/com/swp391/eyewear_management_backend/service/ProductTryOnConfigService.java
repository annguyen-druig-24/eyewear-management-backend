package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.request.ProductTryOnConfigUploadRequest;
import com.swp391.eyewear_management_backend.dto.response.ProductTryOnConfigResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ProductTryOnConfigService {
    ProductTryOnConfigResponse uploadTryOnModel(Long productId,
                                                ProductTryOnConfigUploadRequest request,
                                                MultipartFile modelFile) throws IOException;

    ProductTryOnConfigResponse getByProductId(Long productId);

    void deleteByProductId(Long productId) throws IOException;
}