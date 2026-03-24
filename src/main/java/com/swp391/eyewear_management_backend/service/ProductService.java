package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.request.ProductCreateRequest;
import com.swp391.eyewear_management_backend.dto.request.ProductUpdateRequest;
import com.swp391.eyewear_management_backend.dto.response.BrandResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductService {
    public List<ProductResponse> searchProducts(String productName, Double minPrice, Double maxPrice, String brand);
    public List<ProductResponse> searchProductsByAdmin(String productName, Double minPrice, Double maxPrice, String brand);
    List<BrandResponse> getAllBrands();
    public ProductDetailResponse getProductById(Long id);

    ProductResponse createProduct(ProductCreateRequest request, List<MultipartFile> imageFiles) throws IOException;
    ProductResponse updateProduct(ProductUpdateRequest request);
    void deleteProduct(Long id);
}