package com.closetly.closetly_backend.user.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocalProfileImageStorageService implements ProfileImageStorageService {

    private static final Path ROOT = Path.of("uploads", "profile-images");

    @Override
    public String saveProfileImage(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Only image uploads are allowed");
        }

        String ext = extensionFromContentType(contentType);
        if (ext == null) {
            ext = extensionFromFilename(file.getOriginalFilename());
        }
        if (ext == null) {
            ext = "jpg";
        }

        Path userDir = ROOT.resolve(String.valueOf(userId));
        try {
            Files.createDirectories(userDir);
            String filename = UUID.randomUUID() + "." + ext;
            Path target = userDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/profile-images/" + userId + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    private static String extensionFromContentType(String contentType) {
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("jpeg") || ct.contains("jpg")) return "jpg";
        if (ct.contains("png")) return "png";
        if (ct.contains("webp")) return "webp";
        if (ct.contains("gif")) return "gif";
        return null;
    }

    private static String extensionFromFilename(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return null;
        String ext = filename.substring(idx + 1).toLowerCase(Locale.ROOT);
        if (ext.length() > 8) return null;
        return ext;
    }
}

