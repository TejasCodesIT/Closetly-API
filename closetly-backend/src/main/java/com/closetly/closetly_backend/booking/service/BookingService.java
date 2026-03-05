package com.closetly.closetly_backend.booking.service;

import com.closetly.closetly_backend.booking.dto.BookingRequestDTO;
import com.closetly.closetly_backend.booking.dto.BookingResponseDTO;

import java.util.List;

public interface BookingService {
    BookingResponseDTO createBooking(BookingRequestDTO request, String email);

    BookingResponseDTO approveBooking(Long bookingId, Long sellerId);

    BookingResponseDTO rejectBooking(Long bookingId, Long sellerId);

    List<BookingResponseDTO> getBookingsForProduct(Long productId);
}
