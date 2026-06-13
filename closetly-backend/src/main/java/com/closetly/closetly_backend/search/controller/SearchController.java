package com.closetly.closetly_backend.search.controller;

import com.closetly.closetly_backend.search.dto.SearchHistoryDTO;
import com.closetly.closetly_backend.search.dto.SearchRequestDTO;
import com.closetly.closetly_backend.search.service.SearchHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchHistoryService searchHistoryService;

    @PostMapping
    public ResponseEntity<?> recordSearch(@Valid @RequestBody SearchRequestDTO request, Authentication authentication) {
        if (authentication != null) {
            searchHistoryService.recordSearch(authentication.getName(), request.getQuery());
        }
        return ResponseEntity.ok(Map.of("message", "Recorded"));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SearchHistoryDTO>> history(Authentication authentication) {
        return ResponseEntity.ok(searchHistoryService.getMyHistory(authentication.getName()));
    }
}
