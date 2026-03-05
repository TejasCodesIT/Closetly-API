package com.closetly.closetly_backend.review.controller;

import com.closetly.closetly_backend.review.dto.ReviewDTO;
import com.closetly.closetly_backend.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDTO> create(@Valid @RequestBody ReviewDTO dto) {
        return ResponseEntity.ok(reviewService.createReview(dto));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDTO>> forProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsForProduct(productId));
    }
}
