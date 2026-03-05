package com.closetly.closetly_backend.admin.service;

import com.closetly.closetly_backend.admin.dto.ReviewModerationDTO;
import com.closetly.closetly_backend.admin.entity.ReviewFlag;
import com.closetly.closetly_backend.admin.entity.SystemLog;
import com.closetly.closetly_backend.admin.repository.ReviewFlagRepository;
import com.closetly.closetly_backend.admin.repository.SystemLogRepository;
import com.closetly.closetly_backend.common.ResourceNotFoundException;
import com.closetly.closetly_backend.review.entity.Review;
import com.closetly.closetly_backend.review.repository.ReviewRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewModerationService {

    private final ReviewFlagRepository reviewFlagRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final SystemLogRepository systemLogRepository;

    @Transactional(readOnly = true)
    public Page<ReviewModerationDTO> getFlaggedReviews(String status, Pageable pageable) {
        Page<ReviewFlag> flags = status != null && !status.isEmpty()
                ? reviewFlagRepository.findByStatus(status.toUpperCase(), pageable)
                : reviewFlagRepository.findFlaggedReviews(pageable);

        return flags.map(this::mapToDTO);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        review.setDeleted(true);
        reviewRepository.save(review);

        logAction(SystemLog.LogType.REVIEW_DELETED,
                "Review " + reviewId + " deleted by admin.");
    }

    @Transactional
    public void banUser(Long userId, String banReason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEnabled(false);
        userRepository.save(user);

        logAction(SystemLog.LogType.USER_BANNED,
                "User " + userId + " (" + user.getEmail() + ") banned. Reason: " + banReason);
    }

    private ReviewModerationDTO mapToDTO(ReviewFlag flag) {
        Review review = flag.getReview();
        return ReviewModerationDTO.builder()
                .reviewId(review.getId())
                .productId(review.getProduct().getId())
                .productTitle(review.getProduct().getTitle())
                .reviewerId(review.getReviewer().getId())
                .reviewerUsername(review.getReviewer().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .flagReason(flag.getReason())
                .flagStatus(flag.getStatus().toString())
                .reviewedAt(review.getCreatedAt())
                .build();
    }

    private void logAction(SystemLog.LogType type, String message) {
        SystemLog log = SystemLog.builder()
                .type(type)
                .message(message)
                .build();
        systemLogRepository.save(log);
    }
}
