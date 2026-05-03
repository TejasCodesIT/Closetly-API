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

    public CloudinaryImageData uploadImage(MultipartFile file) {
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
            String publicId = (String) uploadResult.get("public_id");

            if (secureUrl == null) {
                throw new RuntimeException("Cloudinary upload did not return a secure URL");
            }

            log.info("Image uploaded successfully. URL: {}, public_id: {}", secureUrl, publicId);

            return new CloudinaryImageData(secureUrl, publicId);

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

    public void deleteImage(String publicId) {
        try {
            if (publicId == null || publicId.trim().isEmpty()) {
                log.warn("Attempted to delete image with null or empty public_id");
                return;
            }

            log.debug("Deleting image from Cloudinary, public_id: {}", publicId);
            Map deleteResult = cloudinary.uploader().destroy(publicId, Map.of());
            String result = (String) deleteResult.get("result");
            if ("ok".equals(result)) {
                log.info("Image deleted successfully from Cloudinary, public_id: {}", publicId);
            } else {
                log.warn("Cloudinary delete returned non-ok result: {} for public_id: {}", result, publicId);
            }
        } catch (Exception e) {
            log.error("Error deleting image from Cloudinary: public_id={}", publicId, e);
            throw new RuntimeException("Image deletion failed: " + e.getMessage(), e);
        }
    }

    public static class CloudinaryImageData {
        private final String url;
        private final String publicId;

        public CloudinaryImageData(String url, String publicId) {
            this.url = url;
            this.publicId = publicId;
        }

        public String getUrl() {
            return url;
        }

        public String getPublicId() {
            return publicId;
        }
    }
}