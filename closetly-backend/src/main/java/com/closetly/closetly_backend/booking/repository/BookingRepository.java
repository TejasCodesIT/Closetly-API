package com.closetly.closetly_backend.booking.repository;

import com.closetly.closetly_backend.booking.entity.Booking;
import com.closetly.closetly_backend.booking.entity.Booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByProductIdAndStatus(Long productId, BookingStatus status);

    List<Booking> findByProductIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long productId, LocalDate end,
            LocalDate start);

    List<Booking> findByCustomerEmailOrderByCreatedAtDesc(String email);

    List<Booking> findByProductSellerEmailOrderByCreatedAtDesc(String email);

    @Query("""
                SELECT COUNT(b) FROM Booking b
                WHERE b.createdAt >= :startDate AND b.createdAt <= :endDate
                AND b.status IN ('APPROVED', 'COMPLETED')
            """)
    Long countBookingsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
