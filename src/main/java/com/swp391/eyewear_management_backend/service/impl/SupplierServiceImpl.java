package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.request.BrandDto;
import com.swp391.eyewear_management_backend.dto.request.CreateSupplierBrandRequest;
import com.swp391.eyewear_management_backend.dto.response.SupplierResponse;
import com.swp391.eyewear_management_backend.entity.Brand;
import com.swp391.eyewear_management_backend.entity.BrandSupplier;
import com.swp391.eyewear_management_backend.entity.Supplier;
import com.swp391.eyewear_management_backend.repository.BrandRepo;
import com.swp391.eyewear_management_backend.repository.BrandSupplierRepo;
import com.swp391.eyewear_management_backend.repository.SupplierRepository;
import com.swp391.eyewear_management_backend.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Autowired
    private SupplierRepository supplierRepo;

    @Autowired
    private BrandRepo brandRepo;

    @Autowired
    private BrandSupplierRepo brandSupplierRepo;

    @Override
    public List<SupplierResponse> getAllSuppliers() {
        List<Supplier> suppliers = supplierRepository.findAll();

        // Map từ Entity sang DTO
        return suppliers.stream().map(supplier -> SupplierResponse.builder()
                .id(supplier.getSupplierID())
                .name(supplier.getSupplierName()) // Thay đổi getter cho khớp với Entity của bạn
                .phone(supplier.getSupplierPhone())
                .email(null)
                .address(supplier.getSupplierAddress())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createSupplierWithBrands(CreateSupplierBrandRequest request) {
        String normalizedSupplierName = request.getSupplierName() == null ? null : request.getSupplierName().trim();
        if (normalizedSupplierName != null && supplierRepository.existsBySupplierName(normalizedSupplierName)) {
            throw new IllegalArgumentException("Không thể thêm supplier vì tên đã tồn tại trong DB: " + normalizedSupplierName);
        }

//        List<String> duplicatedBrandNames = collectDuplicateBrandNamesInDb(request.getBrands());
//        if (!duplicatedBrandNames.isEmpty()) {
//            throw new IllegalArgumentException("Không thể thêm brand vì đã tồn tại trong DB: " + String.join(", ", duplicatedBrandNames));
//        }

        // 1. Tạo và lưu Supplier mới
        Supplier supplier = new Supplier();
        supplier.setSupplierName(normalizedSupplierName);
        supplier.setSupplierPhone(request.getSupplierPhone());
        supplier.setSupplierAddress(request.getSupplierAddress());

        Supplier savedSupplier = supplierRepo.save(supplier);

//        // 2. Duyệt qua list Brand, lưu từng Brand và tạo quan hệ
//        if (request.getBrands() != null && !request.getBrands().isEmpty()) {
//            for (BrandDto brandDto : request.getBrands()) {
//                // Tạo Brand mới
//                Brand brand = new Brand();
//                brand.setBrandName(brandDto.getBrandName());
//                brand.setDescription(brandDto.getDescription());
//                brand.setLogoUrl(resolveBrandLogoUrl(brandDto));
//                // Set status mặc định là true nếu client không gửi lên
//                brand.setStatus(brandDto.getStatus() != null ? brandDto.getStatus() : true);
//
//                Brand savedBrand = brandRepo.save(brand);
//
//                // 3. Liên kết Brand và Supplier vào bảng Brand_Supplier
//                BrandSupplier brandSupplier = new BrandSupplier();
//                brandSupplier.setSupplier(savedSupplier);
//                brandSupplier.setBrand(savedBrand);
//
//                brandSupplierRepo.save(brandSupplier);
//            }
//        }
    }

    @Override
    @Transactional
    public void addBrandsToExistingSupplier(Long supplierId, List<BrandDto> brands) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Supplier với ID: " + supplierId));

        if (brands != null && !brands.isEmpty()) {
            Set<String> candidateNames = brands.stream()
                    .filter(brandDto -> brandDto != null
                            && brandDto.getBrandName() != null
                            && !brandDto.getBrandName().isBlank())
                    .map(brandDto -> brandDto.getBrandName().trim())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // Lấy toàn bộ brand đã tồn tại theo tên chỉ với 1 query.
            Map<String, Brand> existingBrandsByName = brandRepo.findByBrandNameIn(candidateNames).stream()
                    .collect(Collectors.toMap(
                            brand -> brand.getBrandName().trim(),
                            brand -> brand,
                            (first, second) -> first));

            List<Brand> brandsToCreate = new ArrayList<>();
            List<BrandSupplier> brandSuppliers = new ArrayList<>();
            Set<String> processedNames = new HashSet<>();

            for (BrandDto brandDto : brands) {
                if (brandDto == null || brandDto.getBrandName() == null || brandDto.getBrandName().isBlank()) {
                    continue;
                }

                String normalizedBrandName = brandDto.getBrandName().trim();
                if (!processedNames.add(normalizedBrandName)) {
                    continue;
                }

                // Nếu brand đã có trong DB thì không tạo mới, chỉ map với supplier hiện tại.
                Brand existingBrand = existingBrandsByName.get(normalizedBrandName);
                if (existingBrand != null) {
                    if (!brandSupplierRepo.existsByBrandAndSupplier(existingBrand, supplier)) {
                        BrandSupplier brandSupplier = new BrandSupplier();
                        brandSupplier.setSupplier(supplier);
                        brandSupplier.setBrand(existingBrand);
                        brandSuppliers.add(brandSupplier);
                    }
                    continue;
                }

                Brand newBrand = new Brand();
                newBrand.setBrandName(normalizedBrandName);
                newBrand.setDescription(brandDto.getDescription());
                newBrand.setLogoUrl(null);
                newBrand.setStatus(brandDto.getStatus() != null ? brandDto.getStatus() : true);
                brandsToCreate.add(newBrand);
            }

            if (!brandsToCreate.isEmpty()) {
                List<Brand> savedBrands = brandRepo.saveAll(brandsToCreate);
                for (Brand savedBrand : savedBrands) {
                    BrandSupplier brandSupplier = new BrandSupplier();
                    brandSupplier.setSupplier(supplier);
                    brandSupplier.setBrand(savedBrand);
                    brandSuppliers.add(brandSupplier);
                }
            }

            // Lưu batch các liên kết brand-supplier để giảm số lần ghi DB.
            if (!brandSuppliers.isEmpty()) {
                brandSupplierRepo.saveAll(brandSuppliers);
            }
        }
    }

    private List<String> collectDuplicateBrandNamesInDb(List<BrandDto> brands) {
        //Set<String> duplicatedNames = new LinkedHashSet<>();
        if (brands == null || brands.isEmpty()) {
            return new ArrayList<>();
        }

        // Gom tên brand cần kiểm tra (unique, giữ thứ tự xuất hiện)
        Set<String> candidateNames = new LinkedHashSet<>();
        for (BrandDto brandDto : brands) {
            if (brandDto == null || brandDto.getBrandName() == null || brandDto.getBrandName().isBlank()) {
                continue;
            }
            candidateNames.add(brandDto.getBrandName().trim());
        }

        if (candidateNames.isEmpty()) {
            return new ArrayList<>();
        }

        // Query 1 lần để tránh N query findByBrandName(...)
        Set<String> existingNames = brandRepo.findByBrandNameIn(candidateNames).stream()
                .map(Brand::getBrandName)
                .collect(Collectors.toSet());

//            String brandName = brandDto.getBrandName().trim();
//            if (brandRepo.findByBrandName(brandName).isPresent()) {
//                duplicatedNames.add(brandName);
//            }
        List<String> duplicatedNames = new ArrayList<>();
        for (String candidateName : candidateNames) {
            if (existingNames.contains(candidateName)) {
                duplicatedNames.add(candidateName);
            }
        }

        //return new ArrayList<>(duplicatedNames);
        return duplicatedNames;
    }


}