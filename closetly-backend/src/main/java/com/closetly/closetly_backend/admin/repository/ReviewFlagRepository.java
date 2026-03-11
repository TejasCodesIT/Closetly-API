package com.closetly.closetly_backend.admin.repository;

import com.closetly.closetly_backend.admin.entity.ReviewFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewFlagRepository extends JpaRepository<ReviewFlag, Long> {

    @Query("""
                SELECT rf FROM ReviewFlag rf
                WHERE rf.status = 'PENDING'
                ORDER BY rf.flaggedAt DESC
            """)
    Page<ReviewFlag> findFlaggedReviews(Pageable pageable);

    @Query("""
                SELECT rf FROM ReviewFlag rf
                WHERE rf.status = :status
                ORDER BY rf.flaggedAt DESC
            """)
    Page<ReviewFlag> findByStatus(ReviewFlag.FlagStatus status, Pageable pageable);
}
