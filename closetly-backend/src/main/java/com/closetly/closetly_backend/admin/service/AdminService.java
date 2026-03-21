package com.closetly.closetly_backend.admin.service;

import com.closetly.closetly_backend.admin.dto.*;
import com.closetly.closetly_backend.admin.entity.Report;
import com.closetly.closetly_backend.admin.entity.SystemLog;
import com.closetly.closetly_backend.admin.repository.ReportRepository;
import com.closetly.closetly_backend.admin.repository.SystemLogRepository;
import com.closetly.closetly_backend.booking.entity.Booking;
import com.closetly.closetly_backend.booking.repository.BookingRepository;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BookingRepository bookingRepository;
    private final ReportRepository reportRepository;
    private final SystemLogRepository systemLogRepository;

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToUserDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::mapToProductDTO);
    }

    @Transactional(readOnly = true)
    public Page<BookingDTO> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable).map(this::mapToBookingDTO);
    }

    @Transactional
    public void blockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setBlocked(true);
        userRepository.save(user);
        logAction(SystemLog.LogType.USER_BANNED, "User " + userId + " blocked by admin");
    }

    @Transactional
    public void unblockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setBlocked(false);
        userRepository.save(user);
        logAction(SystemLog.LogType.SYSTEM_ACTION, "User " + userId + " unblocked by admin");
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setDeleted(true);
        productRepository.save(product);
        logAction(SystemLog.LogType.SYSTEM_ACTION, "Product " + productId + " deleted by admin");
    }

    @Transactional
    public void restoreProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setDeleted(false);
        productRepository.save(product);
        logAction(SystemLog.LogType.SYSTEM_ACTION, "Product " + productId + " restored by admin");
    }

    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        logAction(SystemLog.LogType.SYSTEM_ACTION, "Booking " + bookingId + " cancelled by admin");
    }

    @Transactional(readOnly = true)
    public DashboardOverviewDTO getDashboardOverview() {
        long totalUsers = userRepository.count();
        long totalBookings = bookingRepository.count();
        long pendingReviews = 0; // Placeholder until review system is implemented
        long unresolvedReports = reportRepository.countPendingReports();

        return DashboardOverviewDTO.builder()
                .grossBookings(totalBookings)
                .activeResellers(0L) // Placeholder until reseller logic is implemented
                .totalUsers(totalUsers)
                .pendingReviews(pendingReviews)
                .unresolvedReports(unresolvedReports)
                .systemHealth(100) // Default healthy status
                .build();
    }

    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream().map(role -> role.getName().toString()).toList())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .lastLogin(null) // Placeholder until last login is tracked
                .bookingCount(0) // Placeholder until booking count is implemented
                .productCount(0) // Placeholder until product count is implemented
                .build();
    }

    private ProductDTO mapToProductDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getSalePrice() != null ? BigDecimal.valueOf(product.getSalePrice())
                        : (product.getRentPricePerDay() != null ? BigDecimal.valueOf(product.getRentPricePerDay())
                                : (product.getBuyPrice() != null ? BigDecimal.valueOf(product.getBuyPrice())
                                        : BigDecimal.ZERO)))
                .condition(product.getProductCondition())
                .approved(!product.isDeleted())
                .viewCount(product.getPopularity())
                .bookingCount(0) // Placeholder until booking count is implemented
                .build();
    }

    private BookingDTO mapToBookingDTO(Booking booking) {
        return BookingDTO.builder()
                .id(booking.getId())
                .buyerId(booking.getCustomer().getId())
                .buyerName(booking.getCustomer().getFullName())
                .productId(booking.getProduct().getId())
                .productTitle(booking.getProduct().getTitle())
                .totalPrice(BigDecimal.ZERO) // Placeholder until price calculation is implemented
                .status(booking.getStatus().toString())
                .createdAt(booking.getCreatedAt())
                .startDate(booking.getStartDate() != null ? booking.getStartDate().atStartOfDay() : null)
                .endDate(booking.getEndDate() != null ? booking.getEndDate().atStartOfDay() : null)
                .build();
    }

    private void logAction(SystemLog.LogType type, String message) {
        SystemLog log = SystemLog.builder()
                .type(type)
                .message(message)
                .build();
        systemLogRepository.save(log);
    }
}