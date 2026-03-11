package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.LensType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LensTypeRepo extends JpaRepository<LensType, Long> {
    Optional<LensType> findByTypeName(String typeName);
}
