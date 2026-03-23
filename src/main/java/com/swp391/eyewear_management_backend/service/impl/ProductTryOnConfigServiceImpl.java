package com.swp391.eyewear_management_backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.swp391.eyewear_management_backend.dto.request.ProductTryOnConfigUploadRequest;
import com.swp391.eyewear_management_backend.dto.response.ProductTryOnConfigResponse;
import com.swp391.eyewear_management_backend.entity.Product;
import com.swp391.eyewear_management_backend.entity.ProductTryOnConfig;
import com.swp391.eyewear_management_backend.exception.AppException;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import com.swp391.eyewear_management_backend.repository.ProductRepo;
import com.swp391.eyewear_management_backend.repository.ProductTryOnConfigRepo;
import com.swp391.eyewear_management_backend.service.ProductTryOnConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductTryOnConfigServiceImpl implements ProductTryOnConfigService {

    private static final String RESOURCE_TYPE_RAW = "raw";
    private static final String STORAGE_PROVIDER_CLOUDINARY = "CLOUDINARY";
    private static final String MODEL_FORMAT_GLB = "glb";

    private final ProductTryOnConfigRepo productTryOnConfigRepo;
    private final ProductRepo productRepo;
    private final Cloudinary cloudinary;

    @Override
    @Transactional
    public ProductTryOnConfigResponse uploadTryOnModel(Long productId,
                                                       ProductTryOnConfigUploadRequest request,
                                                       MultipartFile modelFile) throws IOException {
        validateModelFile(modelFile);

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductTryOnConfig config = productTryOnConfigRepo.findByProduct_ProductID(productId)
                .orElseGet(ProductTryOnConfig::new);

        Map uploadResult = cloudinary.uploader().upload(
                modelFile.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", RESOURCE_TYPE_RAW,
                        "folder", "product_try_on/" + productId,
                        "public_id", "model",
                        "overwrite", true,
                        "invalidate", true,
                        "use_filename", false,
                        "unique_filename", false,
                        "filename_override", buildSafeFileName(productId)
                )
        );

        config.setProduct(product);
        config.setModelUrl((String) uploadResult.get("secure_url"));
        config.setCloudinaryPublicId((String) uploadResult.get("public_id"));
        config.setCloudinaryAssetId((String) uploadResult.get("asset_id"));
        config.setCloudinaryVersion(String.valueOf(uploadResult.get("version")));
        config.setStorageProvider(STORAGE_PROVIDER_CLOUDINARY);
        config.setModelFormat(resolveFormat(uploadResult, modelFile));

        applyRequest(config, request);

        ProductTryOnConfig savedConfig = productTryOnConfigRepo.save(config);
        return toResponse(savedConfig);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductTryOnConfigResponse getByProductId(Long productId) {
        ProductTryOnConfig config = productTryOnConfigRepo.findByProduct_ProductID(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_TRY_ON_CONFIG_NOT_FOUND));
        return toResponse(config);
    }

    @Override
    @Transactional
    public void deleteByProductId(Long productId) throws IOException {
        ProductTryOnConfig config = productTryOnConfigRepo.findByProduct_ProductID(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_TRY_ON_CONFIG_NOT_FOUND));

        if (config.getCloudinaryPublicId() != null && !config.getCloudinaryPublicId().isBlank()) {
            cloudinary.uploader().destroy(
                    config.getCloudinaryPublicId(),
                    ObjectUtils.asMap(
                            "resource_type", RESOURCE_TYPE_RAW,
                            "invalidate", true
                    )
            );
        }

        productTryOnConfigRepo.delete(config);
    }

    private void validateModelFile(MultipartFile modelFile) {
        if (modelFile == null || modelFile.isEmpty()) {
            throw new AppException(ErrorCode.TRY_ON_MODEL_FILE_REQUIRED);
        }

        String originalFilename = modelFile.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".glb")) {
            throw new AppException(ErrorCode.TRY_ON_MODEL_INVALID_FORMAT);
        }
    }

    private void applyRequest(ProductTryOnConfig config, ProductTryOnConfigUploadRequest request) {
        if (request == null) {
            if (config.getIsEnabled() == null) {
                config.setIsEnabled(false);
            }
            return;
        }

        config.setIsEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : defaultBoolean(config.getIsEnabled(), false));
        config.setPreviewImageUrl(request.getPreviewImageUrl());
        config.setScaleValue(defaultDecimal(request.getScaleValue(), config.getScaleValue(), BigDecimal.ONE));
        config.setOffsetX(defaultDecimal(request.getOffsetX(), config.getOffsetX(), BigDecimal.ZERO));
        config.setOffsetY(defaultDecimal(request.getOffsetY(), config.getOffsetY(), BigDecimal.ZERO));
        config.setOffsetZ(defaultDecimal(request.getOffsetZ(), config.getOffsetZ(), BigDecimal.ZERO));
        config.setRotationX(defaultDecimal(request.getRotationX(), config.getRotationX(), BigDecimal.ZERO));
        config.setRotationY(defaultDecimal(request.getRotationY(), config.getRotationY(), BigDecimal.ZERO));
        config.setRotationZ(defaultDecimal(request.getRotationZ(), config.getRotationZ(), BigDecimal.ZERO));
        config.setNotes(request.getNotes());
        config.setAnchorMode(defaultString(request.getAnchorMode(), config.getAnchorMode(), "NOSE_BRIDGE"));
        config.setScaleMode(defaultString(request.getScaleMode(), config.getScaleMode(), "FACE_WIDTH"));
        config.setFitRatio(defaultDecimal(request.getFitRatio(), config.getFitRatio(), new BigDecimal("0.8200")));
        config.setOffsetIpdX(defaultDecimal(request.getOffsetIpdX(), config.getOffsetIpdX(), BigDecimal.ZERO));
        config.setOffsetIpdY(defaultDecimal(request.getOffsetIpdY(), config.getOffsetIpdY(), BigDecimal.ZERO));
        config.setOffsetIpdZ(defaultDecimal(request.getOffsetIpdZ(), config.getOffsetIpdZ(), BigDecimal.ZERO));
        config.setYawBias(defaultDecimal(request.getYawBias(), config.getYawBias(), BigDecimal.ZERO));
        config.setPitchBias(defaultDecimal(request.getPitchBias(), config.getPitchBias(), BigDecimal.ZERO));
        config.setRollBias(defaultDecimal(request.getRollBias(), config.getRollBias(), BigDecimal.ZERO));
        config.setDepthRatio(defaultDecimal(request.getDepthRatio(), config.getDepthRatio(), BigDecimal.ZERO));

        if (config.getFitRatio().compareTo(new BigDecimal("0.1000")) < 0
                || config.getFitRatio().compareTo(new BigDecimal("3.0000")) > 0) {
            throw new AppException(ErrorCode.TRY_ON_MODEL_INVALID_SCALE);
        }
        if (!"NOSE_BRIDGE".equals(config.getAnchorMode()) && !"EYE_CENTER".equals(config.getAnchorMode())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (!"FACE_WIDTH".equals(config.getScaleMode()) && !"IPD".equals(config.getScaleMode())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (config.getScaleValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.TRY_ON_MODEL_INVALID_SCALE);
        }
    }

    private String defaultString(String newValue, String existingValue, String fallback) {
        if (newValue != null && !newValue.isBlank()) return newValue;
        if (existingValue != null && !existingValue.isBlank()) return existingValue;
        return fallback;
    }

    private BigDecimal defaultDecimal(BigDecimal newValue, BigDecimal existingValue, BigDecimal fallback) {
        if (newValue != null) {
            return newValue;
        }
        if (existingValue != null) {
            return existingValue;
        }
        return fallback.setScale(4);
    }

    private Boolean defaultBoolean(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
    }

    private String resolveFormat(Map uploadResult, MultipartFile modelFile) {
        Object format = uploadResult.get("format");
        if (format != null) {
            return format.toString().toLowerCase();
        }

        String originalFilename = modelFile.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }

        return MODEL_FORMAT_GLB;
    }

    private String buildSafeFileName(Long productId) {
        return "product_" + productId + ".glb";
    }

    private ProductTryOnConfigResponse toResponse(ProductTryOnConfig config) {
        return ProductTryOnConfigResponse.builder()
                .tryOnConfigId(config.getTryOnConfigId())
                .productId(config.getProduct().getProductID())
                .isEnabled(config.getIsEnabled())
                .modelUrl(config.getModelUrl())
                .previewImageUrl(config.getPreviewImageUrl())
                .cloudinaryPublicId(config.getCloudinaryPublicId())
                .cloudinaryAssetId(config.getCloudinaryAssetId())
                .cloudinaryVersion(config.getCloudinaryVersion())
                .storageProvider(config.getStorageProvider())
                .modelFormat(config.getModelFormat())
                .scaleValue(config.getScaleValue())
                .offsetX(config.getOffsetX())
                .offsetY(config.getOffsetY())
                .offsetZ(config.getOffsetZ())
                .rotationX(config.getRotationX())
                .rotationY(config.getRotationY())
                .rotationZ(config.getRotationZ())
                .notes(config.getNotes())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}