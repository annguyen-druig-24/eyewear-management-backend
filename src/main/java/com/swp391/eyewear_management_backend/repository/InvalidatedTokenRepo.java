package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/*
    - Truy vấn bảng token đã vô hiệu hóa.
    - Kế thừa `JpaRepository`.
    - Method quan trọng được dùng trực tiếp:
      + `save(...)` khi logout/refresh.
      + `existsById(jti)` khi verify token.
*/

@Repository
public interface InvalidatedTokenRepo extends JpaRepository<InvalidatedToken, String> {
}
