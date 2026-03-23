package com.closetly.closetly_backend.booking.repository;

import com.closetly.closetly_backend.booking.entity.Booking;
import com.closetly.closetly_backend.booking.entity.Booking.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
        @EntityGraph(attributePaths = { "product", "product.images", "product.seller", "customer" })
        Page<Booking> findAll(Pageable pageable);

        @Query("""
                        SELECT b FROM Booking b
                        JOIN FETCH b.product p
                        LEFT JOIN FETCH p.images
                        WHERE b.product.id = :productId AND b.status = :status
                        """)
        List<Booking> findByProductIdAndStatus(@Param("productId") Long productId,
                        @Param("status") BookingStatus status);

        java.util.Optional<Booking> findByProductIdAndCustomerIdAndStatus(Long productId, Long customerId,
                        BookingStatus status);

        List<Booking> findByProductIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long productId, LocalDate end,
                        LocalDate start);

        @Query("""
                        SELECT b FROM Booking b
                        JOIN FETCH b.product p
                        LEFT JOIN FETCH p.images
                        WHERE b.customer.email = :email
                        ORDER BY b.createdAt DESC
                        """)
        List<Booking> findByCustomerEmailOrderByCreatedAtDesc(@Param("email") String email);

        @Query("""
                        SELECT b FROM Booking b
                        JOIN FETCH b.product p
                        LEFT JOIN FETCH p.images
                        WHERE p.seller.email = :email
                        ORDER BY b.createdAt DESC
                        """)
        List<Booking> findByProductSellerEmailOrderByCreatedAtDesc(@Param("email") String email);

        @Query("""
                            SELECT COUNT(b) FROM Booking b
                            WHERE b.createdAt >= :startDate AND b.createdAt <= :endDate
                            AND b.status IN ('APPROVED', 'COMPLETED')
                        """)
        Long countBookingsByDateRange(
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        long countByCreatedAtAfter(LocalDateTime dateTime);
}
