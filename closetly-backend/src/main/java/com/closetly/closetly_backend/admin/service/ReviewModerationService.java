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
import java.util.List;
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
        public Page<ReviewModerationDTO> getFlaggedReviews(
                        ReviewFlag.FlagStatus status,
                        Pageable pageable) {

                Page<ReviewFlag> flags = (status != null)
                                ? reviewFlagRepository.findByStatus(status, pageable)
                                : reviewFlagRepository.findFlaggedReviews(pageable);

                return flags.map(this::mapToDTO);
        }

        @Transactional
        public void deleteReview(Long reviewId) {
                Long id = java.util.Objects.requireNonNull(reviewId, "reviewId is required");
                Review review = reviewRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

                review.setDeleted(true);
                reviewRepository.save(review);

                logAction(SystemLog.LogType.REVIEW_DELETED,
                                "Review " + reviewId + " deleted by admin.");
        }

        @Transactional
        public void banUser(Long userId, String banReason) {
                Long id = java.util.Objects.requireNonNull(userId, "userId is required");
                User user = userRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                user.setEnabled(false);
                userRepository.save(user);

                logAction(SystemLog.LogType.USER_BANNED,
                                "User " + userId + " (" + user.getEmail() + ") banned. Reason: " + banReason);
        }

        private ReviewModerationDTO mapToDTO(ReviewFlag flag) {
                Review review = flag.getReview();
                return ReviewModerationDTO.builder()
        .id(flag.getId())
        .reviewId(review.getId())
        .productId(review.getProduct().getId())
        .productTitle(review.getProduct().getTitle())
        .reviewerName(review.getReviewer().getFullName())
        .rating(review.getRating())
        .reviewText(review.getComment())
        .flagReason(flag.getReason())
        .reportedFor(List.of(flag.getReason()))
        .status(flag.getStatus().toString())
        .flaggedAt(flag.getFlaggedAt())
        .build();
        }

        private void logAction(SystemLog.LogType type, String message) {
                SystemLog log = SystemLog.builder()
                                .type(type)
                                .message(message)
                                .build();
                systemLogRepository.save(java.util.Objects.requireNonNull(log));
        }

        @Transactional
        public void updateReviewFlagStatus(Long reviewFlagId, ReviewFlag.FlagStatus status) {
                Long id = java.util.Objects.requireNonNull(reviewFlagId, "reviewFlagId is required");
                ReviewFlag flag = reviewFlagRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Review flag not found"));
                flag.setStatus(status);
                reviewFlagRepository.save(flag);

                logAction(SystemLog.LogType.SYSTEM_ACTION,
                                "Review flag " + reviewFlagId + " status changed to " + status);
        }
}
