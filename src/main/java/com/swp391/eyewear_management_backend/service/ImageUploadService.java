package com.swp391.eyewear_management_backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface ImageUploadService {

    public String uploadImage(MultipartFile file) throws IOException;

    public Map deleteImage(String imageUrl) throws IOException;


}
