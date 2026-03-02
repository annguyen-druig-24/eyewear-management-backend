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
}

