package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.request.ProductUpdateRequest;
import com.swp391.eyewear_management_backend.dto.response.ProductDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductResponse;
import com.swp391.eyewear_management_backend.dto.response.extend.ContactLensResponse;
import com.swp391.eyewear_management_backend.dto.response.extend.FrameResponse;
import com.swp391.eyewear_management_backend.dto.response.extend.LensResponse;
import com.swp391.eyewear_management_backend.entity.Brand;
import com.swp391.eyewear_management_backend.entity.Product;
import com.swp391.eyewear_management_backend.entity.ProductType;
import com.swp391.eyewear_management_backend.mapper.ProductMapper;
import com.swp391.eyewear_management_backend.repository.BrandRepo;
import com.swp391.eyewear_management_backend.repository.ProductRepo;
import com.swp391.eyewear_management_backend.repository.ProductTypeRepo;
import com.swp391.eyewear_management_backend.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
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
    private ProductMapper productMapper;

    @Override
    public List<ProductResponse> searchProducts(String name, Double minPrice, Double maxPrice, String brand) {
        List<Product> products = productRepository.searchProducts(name, minPrice, maxPrice, brand);
        return products.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        // MapStruct tự động chọn trả về FrameResponse hay LensResponse
        ProductDetailResponse response = productMapper.toDetailResponse(product);
        
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

        // 3. Xử lý Brand (Thương hiệu)
        if (request.getBrandName() != null && !request.getBrandName().trim().isEmpty()) {
            String brandNameInput = request.getBrandName().trim();
            Brand brand = brandRepository.findByBrandName(brandNameInput)
                    .orElseGet(() -> {
                        // Nếu chưa có thì tạo mới
                        Brand newBrand = new Brand();
                        newBrand.setBrandName(brandNameInput);
                        newBrand.setStatus(true); // Set status mặc định là 1 (Active) dựa theo hình ảnh DB của bạn
                        return brandRepository.save(newBrand);
                    });
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

        // 2. Kiểm tra xem product có đang được sử dụng trong order không
        if (product.getOrderDetails() != null && !product.getOrderDetails().isEmpty()) {
            throw new RuntimeException("Không thể xóa sản phẩm này vì đã có trong đơn hàng. " +
                    "Sản phẩm có " + product.getOrderDetails().size() + " đơn hàng liên quan.");
        }

        // 3. Xóa các liên kết còn lại (Frame, Lens, ContactLens) nếu có
        // Các images và inventories sẽ tự động xóa do orphanRemoval = true
        
        // 4. Thực hiện xóa product
        productRepository.delete(product);
    }

}
