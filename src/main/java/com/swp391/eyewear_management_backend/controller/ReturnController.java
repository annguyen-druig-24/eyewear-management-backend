package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.service.ImageUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    @Autowired
    private ImageUploadService imageUploadService;

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadReturnImage(@RequestParam("returnImage") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy file ảnh đính kèm"));
        }

        try {
            // Gọi service để upload và lấy link
            String imageUrl = imageUploadService.uploadImage(file);

            // --------------------------------------------------------
            // MÔ PHỎNG LƯU DATABASE (Sử dụng Spring Data JPA)
            // ReturnRequestEntity returnRequest = new ReturnRequestEntity();
            // returnRequest.setReason("Hàng không đúng mô tả");
            // returnRequest.setImageUrl(imageUrl);
            // returnRequestRepository.save(returnRequest);
            // --------------------------------------------------------

            // Trả về JSON chứa URL để client sử dụng
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Upload ảnh thành công!");
            response.put("imageUrl", imageUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi server khi xử lý ảnh đổi trả: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete-image")
    public ResponseEntity<?> deleteReturnImage(@RequestParam("imageUrl") String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng cung cấp link ảnh cần xóa"));
        }

        try {
            // Gọi Service để xóa ảnh trên Cloudinary
            Map result = imageUploadService.deleteImage(imageUrl);

            // Kiểm tra kết quả Cloudinary trả về
            if ("ok".equals(result.get("result"))) {

                // --------------------------------------------------------
                // CHỖ NÀY BẠN CÓ THỂ CODE THÊM LOGIC XÓA RECORD TRONG DATABASE
                // Ví dụ: returnRequestRepository.deleteByImageUrl(imageUrl);
                // --------------------------------------------------------

                return ResponseEntity.ok(Map.of("message", "Đã xóa ảnh trên Cloudinary thành công!"));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Không tìm thấy ảnh hoặc không thể xóa: " + result.get("result")));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi server khi xóa ảnh: " + e.getMessage()));
        }
    }
}

