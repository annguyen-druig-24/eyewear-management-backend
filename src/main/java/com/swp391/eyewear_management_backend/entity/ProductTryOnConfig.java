package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Product_Try_On_Config")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"product"})
public class ProductTryOnConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Try_On_Config_ID")
    private Long tryOnConfigId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Product_ID", nullable = false, unique = true)
    private Product product;

    @Column(name = "Is_Enabled", nullable = false)
    private Boolean isEnabled = false;

    @Column(name = "Model_URL", columnDefinition = "NVARCHAR(1000)")
    private String modelUrl;

    @Column(name = "Preview_Image_URL", columnDefinition = "NVARCHAR(1000)")
    private String previewImageUrl;

    @Column(name = "Cloudinary_Public_ID", columnDefinition = "NVARCHAR(255)")
    private String cloudinaryPublicId;

    @Column(name = "Cloudinary_Asset_ID", columnDefinition = "NVARCHAR(255)")
    private String cloudinaryAssetId;

    @Column(name = "Cloudinary_Version", columnDefinition = "NVARCHAR(50)")
    private String cloudinaryVersion;

    @Column(name = "Storage_Provider", nullable = false, columnDefinition = "NVARCHAR(50)")
    private String storageProvider = "CLOUDINARY";

    @Column(name = "Model_Format", nullable = false, columnDefinition = "NVARCHAR(20)")
    private String modelFormat = "glb";

    @Column(name = "Scale_Value", nullable = false, precision = 10, scale = 4)
    private BigDecimal scaleValue = BigDecimal.ONE.setScale(4);

    @Column(name = "Offset_X", nullable = false, precision = 10, scale = 4)
    private BigDecimal offsetX = BigDecimal.ZERO.setScale(4);

    @Column(name = "Offset_Y", nullable = false, precision = 10, scale = 4)
    private BigDecimal offsetY = BigDecimal.ZERO.setScale(4);

    @Column(name = "Offset_Z", nullable = false, precision = 10, scale = 4)
    private BigDecimal offsetZ = BigDecimal.ZERO.setScale(4);

    @Column(name = "Rotation_X", nullable = false, precision = 10, scale = 4)
    private BigDecimal rotationX = BigDecimal.ZERO.setScale(4);

    @Column(name = "Rotation_Y", nullable = false, precision = 10, scale = 4)
    private BigDecimal rotationY = BigDecimal.ZERO.setScale(4);

    @Column(name = "Rotation_Z", nullable = false, precision = 10, scale = 4)
    private BigDecimal rotationZ = BigDecimal.ZERO.setScale(4);

    @Column(name = "Notes", columnDefinition = "NVARCHAR(1000)")
    private String notes;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "Updated_At", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "Anchor_Mode", nullable = false, columnDefinition = "NVARCHAR(30)")
    private String anchorMode = "NOSE_BRIDGE";

    @Column(name = "Scale_Mode", nullable = false, columnDefinition = "NVARCHAR(30)")
    private String scaleMode = "FACE_WIDTH";

    @Column(name = "Fit_Ratio", nullable = false, precision = 10, scale = 4)
    private BigDecimal fitRatio = new BigDecimal("0.8200");

    @Column(name = "Offset_IPD_X", nullable = false, precision = 10, scale = 4)
    private BigDecimal offsetIpdX = BigDecimal.ZERO.setScale(4);

    @Column(name = "Offset_IPD_Y", nullable = false, precision = 10, scale = 4)
    private BigDecimal offsetIpdY = BigDecimal.ZERO.setScale(4);

    @Column(name = "Offset_IPD_Z", nullable = false, precision = 10, scale = 4)
    private BigDecimal offsetIpdZ = BigDecimal.ZERO.setScale(4);

    @Column(name = "Yaw_Bias", nullable = false, precision = 10, scale = 4)
    private BigDecimal yawBias = BigDecimal.ZERO.setScale(4);

    @Column(name = "Pitch_Bias", nullable = false, precision = 10, scale = 4)
    private BigDecimal pitchBias = BigDecimal.ZERO.setScale(4);

    @Column(name = "Roll_Bias", nullable = false, precision = 10, scale = 4)
    private BigDecimal rollBias = BigDecimal.ZERO.setScale(4);

    @Column(name = "Depth_Ratio", nullable = false, precision = 10, scale = 4)
    private BigDecimal depthRatio = BigDecimal.ZERO.setScale(4);

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        normalizeDefaults();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalizeDefaults();
    }

    private void normalizeDefaults() {
        if (isEnabled == null) {
            isEnabled = false;
        }
        if (storageProvider == null || storageProvider.isBlank()) {
            storageProvider = "CLOUDINARY";
        }
        if (modelFormat == null || modelFormat.isBlank()) {
            modelFormat = "glb";
        }
        if (scaleValue == null) {
            scaleValue = BigDecimal.ONE.setScale(4);
        }
        if (offsetX == null) {
            offsetX = BigDecimal.ZERO.setScale(4);
        }
        if (offsetY == null) {
            offsetY = BigDecimal.ZERO.setScale(4);
        }
        if (offsetZ == null) {
            offsetZ = BigDecimal.ZERO.setScale(4);
        }
        if (rotationX == null) {
            rotationX = BigDecimal.ZERO.setScale(4);
        }
        if (rotationY == null) {
            rotationY = BigDecimal.ZERO.setScale(4);
        }
        if (rotationZ == null) {
            rotationZ = BigDecimal.ZERO.setScale(4);
        }
        if (anchorMode == null || anchorMode.isBlank()) {
            anchorMode = "NOSE_BRIDGE";
        }
        if (scaleMode == null || scaleMode.isBlank()) {
            scaleMode = "FACE_WIDTH";
        }
        if (fitRatio == null || fitRatio.compareTo(BigDecimal.ZERO) <= 0) {
            fitRatio = new BigDecimal("0.8200");
        }
        if (offsetIpdX == null) offsetIpdX = BigDecimal.ZERO.setScale(4);
        if (offsetIpdY == null) offsetIpdY = BigDecimal.ZERO.setScale(4);
        if (offsetIpdZ == null) offsetIpdZ = BigDecimal.ZERO.setScale(4);
        if (yawBias == null) yawBias = BigDecimal.ZERO.setScale(4);
        if (pitchBias == null) pitchBias = BigDecimal.ZERO.setScale(4);
        if (rollBias == null) rollBias = BigDecimal.ZERO.setScale(4);
        if (depthRatio == null) depthRatio = BigDecimal.ZERO.setScale(4);
    }
}