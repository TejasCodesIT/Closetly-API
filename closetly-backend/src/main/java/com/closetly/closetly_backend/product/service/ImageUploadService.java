package com.closetly.closetly_backend.product.service;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadService {

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                log.error("Attempted to upload null or empty file");
                throw new IllegalArgumentException("File cannot be null or empty");
            }

            log.debug("Starting image upload for file: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    Map.of("folder", "closetly-products"));

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Image uploaded successfully. URL: {}", secureUrl);

            return secureUrl;

        } catch (IllegalArgumentException e) {
            log.error("Validation error during image upload: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error uploading image to Cloudinary: ", e);
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            throw new RuntimeException("Image upload failed: " + e.getMessage(), e);
        }
    }
}