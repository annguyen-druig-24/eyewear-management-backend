package com.swp391.eyewear_management_backend.service.impl;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ImageUploadService implements com.swp391.eyewear_management_backend.service.ImageUploadService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) throws IOException{
        // Chuyển file thành mảng byte và đẩy lên Cloudinary
        // Chỉ định thư mục lưu trữ là "return_items"
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("folder", "return_items"));

        // Trả về link ảnh HTTPS
        return uploadResult.get("secure_url").toString();
    }

    // HÀM MỚI: Xử lý xóa ảnh dựa vào link URL
    public Map deleteImage(String imageUrl) throws IOException {
        // 1. Tách lấy public_id từ cái link URL
        String publicId = extractPublicIdFromUrl(imageUrl);

        if (publicId == null) {
            throw new IllegalArgumentException("Định dạng link ảnh không hợp lệ");
        }

        // 2. Gọi API của Cloudinary để xóa
        // Hàm destroy() sẽ trả về một Map chứa kết quả, nếu thành công sẽ có key "result" mang giá trị "ok"
        return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    // HÀM PHỤ: Cắt chuỗi để lấy public_id
    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            // Tìm vị trí chữ "/upload/" trong link
            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) return null;

            // Lấy phần đuôi phía sau "/upload/"
            String pathAfterUpload = imageUrl.substring(uploadIndex + 8);

            // Nếu URL có chứa version (ví dụ: v1712345678/), ta phải cắt bỏ phần đó đi
            if (pathAfterUpload.matches("^v\\d+/.*")) {
                pathAfterUpload = pathAfterUpload.substring(pathAfterUpload.indexOf("/") + 1);
            }

            // Cắt bỏ phần đuôi mở rộng (ví dụ: .jpg, .png)
            int dotIndex = pathAfterUpload.lastIndexOf('.');
            if (dotIndex != -1) {
                return pathAfterUpload.substring(0, dotIndex);
            }
            return pathAfterUpload;
        } catch (Exception e) {
            return null;
        }
    }
}

