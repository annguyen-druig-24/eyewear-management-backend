package com.swp391.eyewear_management_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductTryOnConfigResponse {

    @JsonProperty("try_on_config_id")
    Long tryOnConfigId;

    @JsonProperty("product_id")
    Long productId;

    @JsonProperty("is_enabled")
    Boolean isEnabled;

    @JsonProperty("model_url")
    String modelUrl;

    @JsonProperty("preview_image_url")
    String previewImageUrl;

    @JsonProperty("cloudinary_public_id")
    String cloudinaryPublicId;

    @JsonProperty("cloudinary_asset_id")
    String cloudinaryAssetId;

    @JsonProperty("cloudinary_version")
    String cloudinaryVersion;

    @JsonProperty("storage_provider")
    String storageProvider;

    @JsonProperty("model_format")
    String modelFormat;

    @JsonProperty("scale_value")
    BigDecimal scaleValue;

    @JsonProperty("offset_x")
    BigDecimal offsetX;

    @JsonProperty("offset_y")
    BigDecimal offsetY;

    @JsonProperty("offset_z")
    BigDecimal offsetZ;

    @JsonProperty("rotation_x")
    BigDecimal rotationX;

    @JsonProperty("rotation_y")
    BigDecimal rotationY;

    @JsonProperty("rotation_z")
    BigDecimal rotationZ;

    @JsonProperty("notes")
    String notes;

    @JsonProperty("created_at")
    LocalDateTime createdAt;

    @JsonProperty("updated_at")
    LocalDateTime updatedAt;
}
