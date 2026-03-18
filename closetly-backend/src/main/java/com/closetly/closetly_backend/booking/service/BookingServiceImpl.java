package com.closetly.closetly_backend.booking.service;

import com.closetly.closetly_backend.booking.dto.BookingRequestDTO;
import com.closetly.closetly_backend.booking.dto.BookingResponseDTO;
import com.closetly.closetly_backend.booking.entity.Booking;
import com.closetly.closetly_backend.booking.entity.Booking.BookingStatus;
import com.closetly.closetly_backend.booking.repository.BookingRepository;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
public BookingResponseDTO createBooking(BookingRequestDTO request, String email) {

    Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

    User customer = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

    if (!product.allowsRent()) {
        throw new IllegalArgumentException("Product is not available for rent");
    }

    // Prevent seller booking own product
    if (product.getSeller().getId().equals(customer.getId())) {
        throw new IllegalStateException("You cannot book your own product");
    }

    // Validate date logic
    if (request.getStartDate() == null || request.getEndDate() == null) {
        throw new IllegalArgumentException("Start date and end date are required for booking");
    }
    if (request.getStartDate().isAfter(request.getEndDate())) {
        throw new IllegalArgumentException("Start date cannot be after end date");
    }

    // Check overlapping approved bookings
    List<Booking> overlapping = bookingRepository
            .findByProductIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    request.getProductId(),
                    request.getEndDate(),
                    request.getStartDate());

    boolean alreadyBooked = overlapping.stream()
            .anyMatch(b -> b.getStatus() == BookingStatus.APPROVED);

    if (alreadyBooked) {
        throw new IllegalStateException("Product already booked for selected dates");
    }

    Booking booking = Booking.builder()
            .product(product)
            .customer(customer)
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .message(request.getMessage())
            .status(BookingStatus.PENDING)
            .build();

    Booking saved = bookingRepository.save(booking);

    return toDto(saved);
}

    @Override
    public BookingResponseDTO approveBooking(Long bookingId, Long sellerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getProduct().getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("Seller does not own product");
        }
        booking.setStatus(BookingStatus.APPROVED);
        return toDto(bookingRepository.save(booking));
    }

    @Override
    public BookingResponseDTO rejectBooking(Long bookingId, Long sellerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getProduct().getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("Seller does not own product");
        }
        booking.setStatus(BookingStatus.REJECTED);
        return toDto(bookingRepository.save(booking));
    }

    @Override
    public List<BookingResponseDTO> getBookingsForProduct(Long productId) {
        return bookingRepository.findByProductIdAndStatus(productId, BookingStatus.PENDING).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private BookingResponseDTO toDto(Booking b) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setId(b.getId());
        dto.setProductId(b.getProduct().getId());
        dto.setCustomerId(b.getCustomer().getId());
        dto.setStartDate(b.getStartDate());
        dto.setEndDate(b.getEndDate());
        dto.setStatus(b.getStatus().name());
        dto.setMessage(b.getMessage());
        return dto;
    }
}
