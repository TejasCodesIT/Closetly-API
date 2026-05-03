package com.closetly.closetly_backend.support.controller;

import com.closetly.closetly_backend.support.dto.SupportRequestDTO;
import com.closetly.closetly_backend.support.service.SupportRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportRequestController {

    private final SupportRequestService supportRequestService;

    @PostMapping
    public ResponseEntity<Map<String, String>> submitSupportRequest(@Valid @RequestBody SupportRequestDTO request) {
        supportRequestService.saveSupportRequest(request);
        return ResponseEntity.ok(Map.of("message", "Support request submitted successfully"));
    }
}
