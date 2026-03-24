package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.request.ProductCreateRequest;
import com.swp391.eyewear_management_backend.dto.request.ProductUpdateRequest;
import com.swp391.eyewear_management_backend.dto.response.BrandResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductResponse;
import com.swp391.eyewear_management_backend.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductController {
    @Autowired
    private ProductService productService;

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String brand) {

        List<ProductResponse> result = productService.searchProducts(name, minPrice, maxPrice, brand);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<List<ProductResponse>> searchProductsAdmin(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String brand) {

        List<ProductResponse> result = productService.searchProductsByAdmin(name, minPrice, maxPrice, brand);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/brands")
    public ResponseEntity<List<BrandResponse>> getAllBrands() {
        return ResponseEntity.ok(productService.getAllBrands());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping()
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ProductResponse> createProduct(
            @ModelAttribute ProductCreateRequest request,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles) throws IOException {
        return ResponseEntity.ok(productService.createProduct(request, imageFiles));
    }

    @PutMapping()
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ProductResponse> updateProduct(
            @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        // Trả về 204 No Content là chuẩn RESTful cho hành động xóa thành công
        return ResponseEntity.noContent().build();
    }
}
