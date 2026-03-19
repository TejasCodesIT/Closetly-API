package com.closetly.closetly_backend.user.service;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageStorageService {
    String saveProfileImage(MultipartFile file, Long userId);
}

