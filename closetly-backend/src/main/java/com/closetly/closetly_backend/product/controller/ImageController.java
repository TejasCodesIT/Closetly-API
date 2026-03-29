package com.closetly.closetly_backend.product.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.closetly.closetly_backend.product.dto.ImageUploadResponse;
import com.closetly.closetly_backend.product.service.ImageUploadService;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final ImageUploadService imageUploadService;

    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received image upload request for file: {}", file.getOriginalFilename());

            String url = imageUploadService.uploadImage(file);

            ImageUploadResponse response = ImageUploadResponse.builder()
                    .url(url)
                    .message("Image uploaded successfully")
                    .success(true)
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid file upload attempt: {}", e.getMessage());
            ImageUploadResponse response = ImageUploadResponse.builder()
                    .message(e.getMessage())
                    .success(false)
                    .build();
            return ResponseEntity.badRequest().body(response);

        } catch (RuntimeException e) {
            log.error("Failed to upload image: {}", e.getMessage());
            ImageUploadResponse response = ImageUploadResponse.builder()
                    .message(e.getMessage())
                    .success(false)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}