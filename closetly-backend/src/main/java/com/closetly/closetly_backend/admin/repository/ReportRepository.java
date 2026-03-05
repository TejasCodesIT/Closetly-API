package com.closetly.closetly_backend.admin.repository;

import com.closetly.closetly_backend.admin.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);

    @Query("""
                SELECT COUNT(r) FROM Report r WHERE r.status = 'PENDING'
            """)
    Long countPendingReports();

    @Query("""
                SELECT COUNT(DISTINCT r.product.id) FROM Report r
                WHERE r.status = 'APPROVED' AND r.reportedAt >= :startDate
            """)
    Long countApprovedReports(@Param("startDate") LocalDateTime startDate);

    @Query("""
                SELECT COUNT(DISTINCT r.product.id) FROM Report r
                WHERE r.status = 'APPROVED' AND r.reportedAt >= :previousStartDate
                AND r.reportedAt < :startDate
            """)
    Long countApprovedReportsPreviousPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("previousStartDate") LocalDateTime previousStartDate);

    @Query("""
                SELECT r FROM Report r
                WHERE r.status = :status
                ORDER BY r.reportedAt DESC
            """)
    Page<Report> findByStatusOrderByReportedAtDesc(
            @Param("status") Report.ReportStatus status,
            Pageable pageable);

    @Query(value = """
                SELECT r.id as reportId, p.id as productId, p.title as productTitle,
                       (SELECT url FROM product_images pi WHERE pi.product_id = p.id LIMIT 1) as productImage,
                       u.username as sellerUsername, u.email as sellerEmail,
                       r.reason, r.description, r.status, r.reported_at as reportedAt
                FROM reports r
                JOIN products p ON r.product_id = p.id
                JOIN users u ON p.seller_id = u.id
                WHERE r.status = :status
                ORDER BY r.reported_at DESC
                LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<?> findReportedProductsNative(
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset);
}
