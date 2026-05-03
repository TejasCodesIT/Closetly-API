package com.closetly.closetly_backend.user.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Service
public class LocalProfileImageStorageService implements ProfileImageStorageService {

    private static final String PROFILE_FOLDER = "profile-images";
    private final Cloudinary cloudinary;

    public LocalProfileImageStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String saveProfileImage(MultipartFile file, Long userId, String customerName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Only image uploads are allowed");
        }

        String safeName = sanitizeCustomerName(customerName);
        String publicId = String.format("%s/%s_%d", PROFILE_FOLDER, safeName, System.currentTimeMillis());

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", publicId,
                    "overwrite", true,
                    "resource_type", "image",
                    "quality", "auto",
                    "fetch_format", "auto",
                    "format", "jpg"));
            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl == null) {
                throw new RuntimeException("Cloudinary upload did not return a secure URL");
            }
            return secureUrl.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload profile image", e);
        }
    }

    private String sanitizeCustomerName(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            return "customer";
        }
        return customerName.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("(^_+|_+$)", "");
    }
}
