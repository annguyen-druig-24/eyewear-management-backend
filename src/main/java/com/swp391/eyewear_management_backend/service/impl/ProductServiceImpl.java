package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.request.ProductCreateRequest;
import com.swp391.eyewear_management_backend.dto.request.ProductUpdateRequest;
import com.swp391.eyewear_management_backend.dto.response.BrandResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductResponse;
import com.swp391.eyewear_management_backend.dto.response.VirtualTryOnResponse;
import com.swp391.eyewear_management_backend.dto.response.extend.ContactLensResponse;
import com.swp391.eyewear_management_backend.dto.response.extend.FrameResponse;
import com.swp391.eyewear_management_backend.dto.response.extend.LensResponse;
import com.swp391.eyewear_management_backend.entity.*;
import com.swp391.eyewear_management_backend.exception.AppException;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import com.swp391.eyewear_management_backend.mapper.ProductMapper;
import com.swp391.eyewear_management_backend.repository.*;
import com.swp391.eyewear_management_backend.service.ImageUploadService;
import com.swp391.eyewear_management_backend.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepo productRepository;

    @Autowired
    private BrandRepo brandRepository;

    @Autowired
    private ProductTypeRepo productTypeRepository;

    @Autowired
    private ProductImageRepo productImageRepository;

    @Autowired
    private FrameRepo frameRepository;

    @Autowired
    private LensRepo lensRepository;

    @Autowired
    private ContactLensRepo contactLensRepository;

    @Autowired
    private LensTypeRepo lensTypeRepository;

    @Autowired
    private ImageUploadService imageUploadService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductTryOnConfigRepo productTryOnConfigRepo;

    @Override
    public List<ProductResponse> searchProducts(String name, Double minPrice, Double maxPrice, String brand) {
        List<Product> products = productRepository.searchProducts(name, minPrice, maxPrice, brand);
        return products.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> searchProductsByAdmin(String name, Double minPrice, Double maxPrice, String brand) {
        List<Product> products = productRepository.searchProductsOfAdmin(name, minPrice, maxPrice, brand);
        return products.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

        @Override
        public List<BrandResponse> getAllBrands() {
            return brandRepository.findAll().stream()
            .sorted(Comparator.comparing(Brand::getBrandName, String.CASE_INSENSITIVE_ORDER))
            .map(brand -> BrandResponse.builder()
                .id(brand.getBrandID())
                .brandName(brand.getBrandName())
                .description(brand.getDescription())
                .logoUrl(brand.getLogoUrl())
                .build())
            .collect(Collectors.toList());
        }

    @Override
    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // MapStruct tự động chọn trả về FrameResponse hay LensResponse
        ProductDetailResponse response = productMapper.toDetailResponse(product);
        response.setVirtualTryOn(buildVirtualTryOn(product.getProductID()));
        
        // Populate related products based on type
        if (response instanceof FrameResponse) {
            FrameResponse frameResponse = (FrameResponse) response;
            frameResponse.setFrameId(product.getFrame().getFrameID());
            populateRelatedProducts(frameResponse, product.getProductID());
        } else if (response instanceof LensResponse) {
            LensResponse lensResponse = (LensResponse) response;
            lensResponse.setLensId(product.getLens().getLensID());
            populateRelatedProducts(lensResponse, product.getProductID());
        } else if (response instanceof ContactLensResponse) {
            ContactLensResponse contactLensResponse = (ContactLensResponse) response;
            contactLensResponse.setContactLensId(product.getContactLens().getContactLensID());
            populateRelatedProducts(contactLensResponse, product.getProductID());
        }
        
        return response;
    }
    
    private void populateRelatedProducts(FrameResponse response, Long currentProductId) {
        // Lấy 4 gọng khác
        List<Product> relatedFrames = productRepository.findByProductTypeNameExcludingId("Gọng kính", currentProductId)
                .stream().limit(4).collect(Collectors.toList());
        response.setRelatedFrames(relatedFrames.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList()));
        
        // Lấy 4 tròng kính
        List<Product> relatedLenses = productRepository.findByProductTypeNameExcludingId("Tròng kính", currentProductId)
                .stream().limit(4).collect(Collectors.toList());
        response.setRelatedLenses(relatedLenses.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList()));
    }
    
    private void populateRelatedProducts(LensResponse response, Long currentProductId) {
        // Lấy 4 tròng kính khác
        List<Product> relatedLenses = productRepository.findByProductTypeNameExcludingId("Tròng kính", currentProductId)
                .stream().limit(4).collect(Collectors.toList());
        response.setRelatedLenses(relatedLenses.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList()));
        
        // Lấy 4 gọng kính
        List<Product> relatedFrames = productRepository.findByProductTypeNameExcludingId("Gọng kính", currentProductId)
                .stream().limit(4).collect(Collectors.toList());
        response.setRelatedFrames(relatedFrames.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList()));
    }
    
    private void populateRelatedProducts(ContactLensResponse response, Long currentProductId) {
        // Lấy 4 kính áp tròng khác
        List<Product> relatedContactLenses = productRepository.findByProductTypeNameExcludingId("Kính áp tròng", currentProductId)
                .stream().limit(4).collect(Collectors.toList());
        response.setRelatedContactLenses(relatedContactLenses.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList()));
    }

    private VirtualTryOnResponse buildVirtualTryOn(Long productId) {
        Optional<ProductTryOnConfig> configOptional = productTryOnConfigRepo.findByProduct_ProductID(productId);

        if (configOptional.isEmpty()) {
            return VirtualTryOnResponse.builder()
                    .enabled(false)
                    .modelFormat("glb")
                    .scale(BigDecimal.ONE)
                    .offsetX(BigDecimal.ZERO)
                    .offsetY(BigDecimal.ZERO)
                    .offsetZ(BigDecimal.ZERO)
                    .rotationX(BigDecimal.ZERO)
                    .rotationY(BigDecimal.ZERO)
                    .rotationZ(BigDecimal.ZERO)
                    .build();
        }

        ProductTryOnConfig config = configOptional.get();
        return VirtualTryOnResponse.builder()
                .enabled(Boolean.TRUE.equals(config.getIsEnabled()))
                .modelUrl(config.getModelUrl())
                .modelFormat(config.getModelFormat())
                .scale(config.getScaleValue())
                .offsetX(config.getOffsetX())
                .offsetY(config.getOffsetY())
                .offsetZ(config.getOffsetZ())
                .rotationX(config.getRotationX())
                .rotationY(config.getRotationY())
                .rotationZ(config.getRotationZ())
                .anchorMode(config.getAnchorMode())
                .scaleMode(config.getScaleMode())
                .fitRatio(config.getFitRatio())
                .offsetIpdX(config.getOffsetIpdX())
                .offsetIpdY(config.getOffsetIpdY())
                .offsetIpdZ(config.getOffsetIpdZ())
                .yawBias(config.getYawBias())
                .pitchBias(config.getPitchBias())
                .rollBias(config.getRollBias())
                .depthRatio(config.getDepthRatio())
                .build();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ProductResponse createProduct(ProductCreateRequest request, List<MultipartFile> imageFiles) throws IOException {
        // 1. Kiểm tra SKU bắt buộc phải nhập
        if (request.getSku() == null || request.getSku().trim().isEmpty()) {
            throw new AppException(ErrorCode.SKU_REQUIRED);
        }
        
        // 2. Kiểm tra SKU đã tồn tại chưa
        String skuInput = request.getSku().trim();
        if (productRepository.existsBySKU(skuInput)) {
            throw new AppException(ErrorCode.SKU_ALREADY_EXISTS);
        }
        
        // 3. Tạo product mới
        Product product = new Product();
        
        // 4. Set thông tin cơ bản
        product.setProductName(request.getName());
        product.setSKU(request.getSku());
        product.setPrice(BigDecimal.valueOf(request.getPrice()));
        product.setCostPrice(request.getCostPrice() != null ? 
                BigDecimal.valueOf(request.getCostPrice()) : BigDecimal.valueOf(request.getPrice()));
        product.setDescription(request.getDescription());
        product.setAllowPreorder(request.getAllowPreorder() != null ? request.getAllowPreorder() : false);
        product.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        
        // 5. Xử lý Brand (Thương hiệu) - Chỉ tìm trong DB, không tạo mới
        if (request.getBrandName() == null || request.getBrandName().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Tên thương hiệu không được để trống");
        }
        String brandNameInput = request.getBrandName().trim();
        Brand brand = brandRepository.findByBrandName(brandNameInput)
                .orElseThrow(() -> new AppException(
                        ErrorCode.INVALID_REQUEST,
                        "Thương hiệu '" + brandNameInput + "' không tồn tại trong hệ thống"
                ));
        product.setBrand(brand);
        
        // 6. Xử lý Product Type (Loại sản phẩm) - Chỉ tìm trong DB, không tạo mới
        if (request.getTypeName() == null || request.getTypeName().trim().isEmpty()) {
            throw new RuntimeException("Loại sản phẩm không được để trống");
        }
        String typeNameInput = request.getTypeName().trim();
        ProductType type = productTypeRepository.findByTypeName(typeNameInput)
                .orElseThrow(() -> new RuntimeException("Loại sản phẩm '" + typeNameInput + "' không tồn tại trong hệ thống"));
        product.setProductType(type);
        
        // 7. Lưu product
        Product savedProduct = productRepository.save(product);
        
        // 7.1. Kiểm tra loại sản phẩm và lưu vào bảng tương ứng
        String productTypeName = typeNameInput.trim();
        
        if (productTypeName.equalsIgnoreCase("Gọng kính")) {
            // Validate Frame fields (trừ description)
            if (request.getFrameColor() == null || request.getFrameColor().trim().isEmpty()) {
                throw new RuntimeException("Màu sắc gọng kính không được để trống");
            }
            if (request.getFrameTempleLength() == null) {
                throw new RuntimeException("Chiều dài càng gọng kính không được để trống");
            }
            if (request.getFrameLensWidth() == null) {
                throw new RuntimeException("Chiều rộng tròng gọng kính không được để trống");
            }
            if (request.getFrameBridgeWidth() == null) {
                throw new RuntimeException("Chiều rộng cầu mũi gọng kính không được để trống");
            }
            if (request.getFrameShapeName() == null || request.getFrameShapeName().trim().isEmpty()) {
                throw new RuntimeException("Hình dạng gọng kính không được để trống");
            }
            if (request.getFrameMaterialName() == null || request.getFrameMaterialName().trim().isEmpty()) {
                throw new RuntimeException("Chất liệu gọng kính không được để trống");
            }
            
            // Tạo Frame
            Frame frame = new Frame();
            frame.setProduct(savedProduct);
            frame.setColor(request.getFrameColor().trim());
            frame.setTempleLength(request.getFrameTempleLength());
            frame.setLensWidth(request.getFrameLensWidth());
            frame.setBridgeWidth(request.getFrameBridgeWidth());
            frame.setFrameShapeName(request.getFrameShapeName().trim());
            frame.setFrameMaterialName(request.getFrameMaterialName().trim());
            frame.setDescription(request.getFrameDescription());
            frameRepository.save(frame);
            
        } else if (productTypeName.equalsIgnoreCase("Tròng kính")) {
            // Validate Lens fields (trừ description)
            if (request.getLensTypeName() == null || request.getLensTypeName().trim().isEmpty()) {
                throw new RuntimeException("Loại tròng kính không được để trống");
            }
            if (request.getLensIndexValue() == null) {
                throw new RuntimeException("Chỉ số khúc xạ tròng kính không được để trống");
            }
            if (request.getLensDiameter() == null) {
                throw new RuntimeException("Đường kính tròng kính không được để trống");
            }
            if (request.getLensAvailablePowerRange() == null || request.getLensAvailablePowerRange().trim().isEmpty()) {
                throw new RuntimeException("Phạm vi độ tròng kính không được để trống");
            }
            if (request.getLensIsBlueLightBlock() == null) {
                throw new RuntimeException("Thông tin chống ánh sáng xanh của tròng kính không được để trống");
            }
            if (request.getLensIsPhotochromic() == null) {
                throw new RuntimeException("Thông tin đổi màu của tròng kính không được để trống");
            }
            
            // Tạo Lens
            Lens lens = new Lens();
            lens.setProduct(savedProduct);
            
            // Xử lý LensType - Chỉ tìm trong DB, không tạo mới
            String lensTypeNameInput = request.getLensTypeName().trim();
            LensType lensType = lensTypeRepository.findByTypeName(lensTypeNameInput)
                    .orElseThrow(() -> new RuntimeException("Loại tròng kính '" + lensTypeNameInput + "' không tồn tại trong hệ thống"));
            lens.setLensType(lensType);
            
            lens.setIndexValue(request.getLensIndexValue());
            lens.setDiameter(request.getLensDiameter());
            lens.setAvailablePowerRange(request.getLensAvailablePowerRange().trim());
            lens.setIsBlueLightBlock(request.getLensIsBlueLightBlock());
            lens.setIsPhotochromic(request.getLensIsPhotochromic());
            lens.setDescription(request.getLensDescription());
            lensRepository.save(lens);
            
        } else if (productTypeName.equalsIgnoreCase("Kính áp tròng")) {
            // Validate ContactLens fields (không có description riêng)
            if (request.getContactLensUsageType() == null || request.getContactLensUsageType().trim().isEmpty()) {
                throw new RuntimeException("Loại sử dụng kính áp tròng không được để trống");
            }
            if (request.getContactLensBaseCurve() == null) {
                throw new RuntimeException("Độ cong đáy kính áp tròng không được để trống");
            }
            if (request.getContactLensDiameter() == null) {
                throw new RuntimeException("Đường kính kính áp tròng không được để trống");
            }
            if (request.getContactLensWaterContent() == null) {
                throw new RuntimeException("Hàm lượng nước kính áp tròng không được để trống");
            }
            if (request.getContactLensAvailablePowerRange() == null || request.getContactLensAvailablePowerRange().trim().isEmpty()) {
                throw new RuntimeException("Phạm vi độ kính áp tròng không được để trống");
            }
            if (request.getContactLensQuantityPerBox() == null) {
                throw new RuntimeException("Số lượng kính áp tròng trong hộp không được để trống");
            }
            if (request.getContactLensMaterial() == null || request.getContactLensMaterial().trim().isEmpty()) {
                throw new RuntimeException("Chất liệu kính áp tròng không được để trống");
            }
            if (request.getContactLensReplacementSchedule() == null || request.getContactLensReplacementSchedule().trim().isEmpty()) {
                throw new RuntimeException("Lịch thay thế kính áp tròng không được để trống");
            }
            if (request.getContactLensColor() == null || request.getContactLensColor().trim().isEmpty()) {
                throw new RuntimeException("Màu sắc kính áp tròng không được để trống");
            }
            
            // Tạo ContactLens
            ContactLens contactLens = new ContactLens();
            contactLens.setProduct(savedProduct);
            contactLens.setUsageType(request.getContactLensUsageType().trim());
            contactLens.setBaseCurve(request.getContactLensBaseCurve());
            contactLens.setDiameter(request.getContactLensDiameter());
            contactLens.setWaterContent(request.getContactLensWaterContent());
            contactLens.setAvailablePowerRange(request.getContactLensAvailablePowerRange().trim());
            contactLens.setQuantityPerBox(request.getContactLensQuantityPerBox());
            contactLens.setLensMaterial(request.getContactLensMaterial().trim());
            contactLens.setReplacementSchedule(request.getContactLensReplacementSchedule().trim());
            contactLens.setColor(request.getContactLensColor().trim());
            contactLensRepository.save(contactLens);
        }
        
        // 8. Xử lý upload nhiều ảnh nếu có
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (int i = 0; i < imageFiles.size(); i++) {
                MultipartFile file = imageFiles.get(i);
                if (file != null && !file.isEmpty()) {
                    String imageUrl = imageUploadService.uploadImage(file);
                    // Ảnh đầu tiên sẽ là ảnh đại diện (isAvatar = true)
                    boolean isAvatar = (i == 0);
                    ProductImage productImage = new ProductImage(savedProduct, imageUrl, isAvatar);
                    productImageRepository.save(productImage);
                }
            }
        }
        
        // 9. Trả về response
        return productMapper.toProductResponse(savedProduct);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ProductResponse updateProduct(ProductUpdateRequest request) {
        // 1. Tìm sản phẩm hiện tại
        Product product = productRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + request.getId()));

        // 2. Cập nhật thông tin cơ bản
        if (request.getName() != null) product.setProductName(request.getName());
        if (request.getSku() != null) product.setSKU(request.getSku());
        if (request.getPrice() != null) product.setPrice(BigDecimal.valueOf(request.getPrice()));
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getIsActive() != null) product.setIsActive(request.getIsActive());

        // 3. Xử lý Brand (Thương hiệu)
        if (request.getBrandName() != null && !request.getBrandName().trim().isEmpty()) {
            String brandNameInput = request.getBrandName().trim();
            Brand brand = brandRepository.findByBrandName(brandNameInput)
                    .orElseThrow(() -> new AppException(
                            ErrorCode.INVALID_REQUEST,
                            "Thương hiệu '" + brandNameInput + "' không tồn tại trong hệ thống"
                    ));
            product.setBrand(brand);
        }

        // 4. Xử lý Product Type (Loại sản phẩm)
        if (request.getTypeName() != null && !request.getTypeName().trim().isEmpty()) {
            String typeNameInput = request.getTypeName().trim();
            ProductType type = productTypeRepository.findByTypeName(typeNameInput)
                    .orElseGet(() -> {
                        // Nếu chưa có thì tạo mới
                        ProductType newType = new ProductType();
                        newType.setTypeName(typeNameInput);
                        // Có thể thêm description mặc định nếu cần
                        return productTypeRepository.save(newType);
                    });
            product.setProductType(type);
        }

        // 5. Lưu sản phẩm và trả về
        Product updatedProduct = productRepository.save(product);
        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public void deleteProduct(Long id) {
        // 1. Tìm sản phẩm
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        // 2. Soft delete: Đặt isActive thành false thay vì xóa thật
        product.setIsActive(false);
        productRepository.save(product);
    }

}
