package com.closetly.closetly_backend.admin.service;

import com.closetly.closetly_backend.admin.dto.DashboardOverviewDTO;
import com.closetly.closetly_backend.admin.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private final EntityManager entityManager;
    private final ReportRepository reportRepository;

    public DashboardOverviewDTO getOverview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfCurrentMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfPreviousMonth = startOfCurrentMonth.minusMonths(1);
        LocalDateTime endOfPreviousMonth = startOfCurrentMonth.minusSeconds(1);

        // Gross Bookings
        Long currentBookings = getBookingCount(startOfCurrentMonth, now);
        Long previousBookings = getBookingCount(startOfPreviousMonth, endOfPreviousMonth);
        Double bookingGrowth = calculateGrowth(previousBookings, currentBookings);

        // Active Resellers
        Long currentResellers = getActiveResellerCount(startOfCurrentMonth, now);
        Long previousResellers = getActiveResellerCount(startOfPreviousMonth, endOfPreviousMonth);
        Double resellerGrowth = calculateGrowth(previousResellers, currentResellers);

        // Reported Items
        Long currentReports = reportRepository.countApprovedReports(startOfCurrentMonth);
        Long previousReports = reportRepository.countApprovedReportsPreviousPeriod(
                startOfCurrentMonth,
                startOfPreviousMonth);
        Double reportGrowth = calculateGrowth(previousReports, currentReports);

        // Marketplace Revenue
        Double currentRevenue = getMarketplaceRevenue(startOfCurrentMonth, now);
        Double previousRevenue = getMarketplaceRevenue(startOfPreviousMonth, endOfPreviousMonth);
        Double revenueGrowth = calculateRevenueGrowth(previousRevenue, currentRevenue);

        return DashboardOverviewDTO.builder()
                .grossBookings(currentBookings)
                .grossBookingsGrowth(bookingGrowth)
                .activeResellers(currentResellers)
                .resellerGrowth(resellerGrowth)
                .reportedItems(currentReports)
                .reportedGrowth(reportGrowth)
                .marketplaceRevenue(currentRevenue)
                .revenueGrowth(revenueGrowth)
                .build();
    }

    private Long getBookingCount(LocalDateTime startDate, LocalDateTime endDate) {
        String jpql = """
                    SELECT COUNT(b) FROM Booking b
                    WHERE b.createdAt >= :startDate AND b.createdAt <= :endDate
                """;
        return (Long) entityManager.createQuery(jpql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();
    }

    private Long getActiveResellerCount(LocalDateTime startDate, LocalDateTime endDate) {
        String jpql = """
                    SELECT COUNT(DISTINCT p.seller.id) FROM Product p
                    WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate
                    AND p.status = 'ACTIVE'
                """;
        return (Long) entityManager.createQuery(jpql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();
    }

    private Double getMarketplaceRevenue(LocalDateTime startDate, LocalDateTime endDate) {

        String jpql = """
                SELECT COALESCE(SUM(
                    (FUNCTION('DATEDIFF', b.endDate, b.startDate)) * p.rentPricePerDay
                ), 0.0)
                FROM Booking b
                JOIN b.product p
                WHERE b.createdAt >= :startDate
                  AND b.createdAt <= :endDate
                  AND b.status IN ('APPROVED', 'COMPLETED')
                """;

        Double revenue = entityManager.createQuery(jpql, Double.class)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

        return revenue != null ? revenue * 0.10 : 0.0; // 10% commission
    }

    private Double calculateGrowth(Long previousValue, Long currentValue) {
        if (previousValue == null || previousValue == 0) {
            return currentValue != null && currentValue > 0 ? 100.0 : 0.0;
        }
        return ((currentValue - previousValue) / (double) previousValue) * 100;
    }

    private Double calculateRevenueGrowth(Double previousValue, Double currentValue) {
        if (previousValue == null || previousValue == 0) {
            return currentValue != null && currentValue > 0 ? 100.0 : 0.0;
        }
        return ((currentValue - previousValue) / previousValue) * 100;
    }
}
