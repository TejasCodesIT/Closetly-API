package com.closetly.closetly_backend.booking.repository;

import com.closetly.closetly_backend.booking.entity.Booking;
import com.closetly.closetly_backend.booking.entity.Booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByProductIdAndStatus(Long productId, BookingStatus status);

    List<Booking> findByProductIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long productId, LocalDate end,
            LocalDate start);
}
