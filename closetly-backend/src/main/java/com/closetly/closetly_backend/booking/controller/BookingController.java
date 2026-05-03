package com.closetly.closetly_backend.booking.controller;

import com.closetly.closetly_backend.booking.dto.BookingRequestDTO;
import com.closetly.closetly_backend.booking.dto.BookingResponseDTO;
import com.closetly.closetly_backend.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(
            @Valid @RequestBody BookingRequestDTO request,
            Authentication authentication) {

        String email = authentication.getName(); // logged-in user email

        BookingResponseDTO response = bookingService.createBooking(request, email);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<BookingResponseDTO> approve(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(bookingService.approveBooking(id, authentication.getName()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<BookingResponseDTO> reject(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(bookingService.rejectBooking(id, authentication.getName()));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<BookingResponseDTO>> forProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(bookingService.getBookingsForProduct(productId));
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<BookingResponseDTO>> myRequests(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getMyRequests(authentication.getName()));
    }

    @GetMapping("/my-products")
    public ResponseEntity<List<BookingResponseDTO>> myProducts(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getBookingsOnMyProducts(authentication.getName()));
    }

    // Cancel booking (customer or seller)
    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(@PathVariable Long bookingId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId, email, reason));
    }
}
