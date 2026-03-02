package com.swp391.eyewear_management_backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageUploadService {

    public String uploadImage(MultipartFile file) throws IOException;
}
