package com.closetly.closetly_backend.review.service;

import com.closetly.closetly_backend.review.dto.ReviewDTO;
import com.closetly.closetly_backend.review.entity.Review;
import com.closetly.closetly_backend.review.repository.ReviewRepository;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public ReviewDTO createReview(ReviewDTO reviewDTO) {
        Product product = productRepository.findById(reviewDTO.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        User reviewer = userRepository.findById(reviewDTO.getReviewerId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Review review = Review.builder()
                .product(product)
                .reviewer(reviewer)
                .rating(reviewDTO.getRating())
                .comment(reviewDTO.getComment())
                .build();
        Review saved = reviewRepository.save(review);
        reviewDTO.setId(saved.getId());
        return reviewDTO;
    }

    @Override
    public List<ReviewDTO> getReviewsForProduct(Long productId) {
        return reviewRepository.findByProductId(productId).stream()
                .map(r -> {
                    ReviewDTO dto = new ReviewDTO();
                    dto.setId(r.getId());
                    dto.setProductId(r.getProduct().getId());
                    dto.setReviewerId(r.getReviewer().getId());
                    dto.setRating(r.getRating());
                    dto.setComment(r.getComment());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
