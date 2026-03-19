package com.closetly.closetly_backend.booking.service;

import com.closetly.closetly_backend.booking.dto.BookingRequestDTO;
import com.closetly.closetly_backend.booking.dto.BookingResponseDTO;
import com.closetly.closetly_backend.booking.entity.Booking;
import com.closetly.closetly_backend.booking.entity.Booking.BookingStatus;
import com.closetly.closetly_backend.booking.repository.BookingRepository;
import com.closetly.closetly_backend.notification.service.NotificationService;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import com.closetly.closetly_backend.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Override
public BookingResponseDTO createBooking(BookingRequestDTO request, String email) {

    Long productId = Objects.requireNonNull(request.getProductId(), "productId is required");
    Product product = productRepository.findById(productId)
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
                    productId,
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

    Booking saved = Objects.requireNonNull(bookingRepository.save(booking));

    // Notify product owner (email + optional in-app notification)
    String ownerEmail = saved.getProduct().getSeller().getEmail();
    safeSend(() -> emailService.sendBookingCreatedEmail(
            ownerEmail,
            saved.getProduct().getTitle(),
            customer.getFullName(),
            saved.getStartDate(),
            saved.getEndDate(),
            saved.getMessage()));
    safeSend(() -> notificationService.notifyUserByEmail(ownerEmail,
            "New booking request for \"" + saved.getProduct().getTitle() + "\""));

    return toDto(saved);
}

    @Override
    public BookingResponseDTO approveBooking(Long bookingId, String email) {
        Long id = Objects.requireNonNull(bookingId, "bookingId is required");
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        enforceOwner(booking, email);
        booking.setStatus(BookingStatus.APPROVED);
        Booking saved = bookingRepository.save(booking);

        // Notify customer
        String customerEmail = saved.getCustomer().getEmail();
        safeSend(() -> emailService.sendBookingApprovedEmail(
                customerEmail,
                saved.getProduct().getTitle(),
                saved.getStartDate(),
                saved.getEndDate()));
        safeSend(() -> notificationService.notifyUserByEmail(customerEmail,
                "Your booking was approved for \"" + saved.getProduct().getTitle() + "\""));

        return toDto(saved);
    }

    @Override
    public BookingResponseDTO rejectBooking(Long bookingId, String email) {
        Long id = Objects.requireNonNull(bookingId, "bookingId is required");
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        enforceOwner(booking, email);
        booking.setStatus(BookingStatus.REJECTED);
        Booking saved = bookingRepository.save(booking);

        // Notify customer
        String customerEmail = saved.getCustomer().getEmail();
        safeSend(() -> emailService.sendBookingRejectedEmail(
                customerEmail,
                saved.getProduct().getTitle(),
                saved.getStartDate(),
                saved.getEndDate()));
        safeSend(() -> notificationService.notifyUserByEmail(customerEmail,
                "Your booking was rejected for \"" + saved.getProduct().getTitle() + "\""));

        return toDto(saved);
    }

    @Override
    public List<BookingResponseDTO> getBookingsForProduct(Long productId) {
        Long id = Objects.requireNonNull(productId, "productId is required");
        return bookingRepository.findByProductIdAndStatus(id, BookingStatus.PENDING).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingResponseDTO> getMyRequests(String email) {
        return bookingRepository.findByCustomerEmailOrderByCreatedAtDesc(email).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingResponseDTO> getBookingsOnMyProducts(String email) {
        return bookingRepository.findByProductSellerEmailOrderByCreatedAtDesc(email).stream()
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

    private void enforceOwner(Booking booking, String email) {
        String ownerEmail = booking.getProduct().getSeller().getEmail();
        if (email == null || !email.equalsIgnoreCase(ownerEmail)) {
            throw new AccessDeniedException("Only the product owner can perform this action");
        }
    }

    private static void safeSend(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            // Don't block booking state changes if notifications fail
            System.err.println("Notification/email failed: " + e.getMessage());
        }
    }
}
