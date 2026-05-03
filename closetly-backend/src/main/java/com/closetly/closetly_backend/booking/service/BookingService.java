package com.closetly.closetly_backend.booking.service;

import com.closetly.closetly_backend.booking.dto.BookingRequestDTO;
import com.closetly.closetly_backend.booking.dto.BookingResponseDTO;

import java.util.List;

public interface BookingService {
    BookingResponseDTO createBooking(BookingRequestDTO request, String email);

    BookingResponseDTO approveBooking(Long bookingId, String email);

    BookingResponseDTO rejectBooking(Long bookingId, String email);

    List<BookingResponseDTO> getBookingsForProduct(Long productId);

    List<BookingResponseDTO> getMyRequests(String email);

    List<BookingResponseDTO> getBookingsOnMyProducts(String email);

    // NEW: Cancel booking (customer or seller)
    BookingResponseDTO cancelBooking(Long bookingId, String email, String reason);
}
