package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.request.BrandDto;
import com.swp391.eyewear_management_backend.dto.request.CreateSupplierBrandRequest;
import com.swp391.eyewear_management_backend.dto.response.SupplierResponse;
import com.swp391.eyewear_management_backend.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@CrossOrigin("*")
public class SupplierController {

    private final SupplierService supplierService;

    // API lấy toàn bộ danh sách nhà cung cấp
    @GetMapping
    public ResponseEntity<List<SupplierResponse>> getAllSuppliers() {
        List<SupplierResponse> suppliers = supplierService.getAllSuppliers();
        return ResponseEntity.ok(suppliers);
    }

    @PostMapping(value = "/with-brands", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createSupplierWithBrands(@ModelAttribute CreateSupplierBrandRequest request) {
        try {
            supplierService.createSupplierWithBrands(request);
            return new ResponseEntity<>("Thêm mới Supplier và Brands thành công!", HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Lỗi khi thêm dữ liệu: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // THÊM MỚI ENDPOINT NÀY
    @PostMapping(value = "/{supplierId}/brands", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addBrandsToExistingSupplier(
            @PathVariable Long supplierId,
            @RequestBody List<BrandDto> brands) {
        try {
            supplierService.addBrandsToExistingSupplier(supplierId, brands);
            return new ResponseEntity<>("Thêm Brands cho Supplier thành công!", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Lỗi hệ thống: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}