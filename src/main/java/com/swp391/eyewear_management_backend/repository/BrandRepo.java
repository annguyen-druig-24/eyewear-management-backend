package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BrandRepo extends JpaRepository<Brand, Long> {
    Optional<Brand> findByBrandName(String brandName); // Lưu ý tên field trong entity của bạn
    List<Brand> findByBrandNameIn(Collection<String> brandNames);   //Hàm này thêm vào để tối ưu hóa Độ Phức Tạp
    List<Brand> findByStatusTrue();
}
