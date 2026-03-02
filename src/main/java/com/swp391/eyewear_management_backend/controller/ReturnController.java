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
}
