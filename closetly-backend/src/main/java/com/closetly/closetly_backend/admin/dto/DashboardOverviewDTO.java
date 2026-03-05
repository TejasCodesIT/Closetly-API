package com.closetly.closetly_backend.admin.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDTO {
    private Long grossBookings;
    private Double grossBookingsGrowth;
    private Long activeResellers;
    private Double resellerGrowth;
    private Long reportedItems;
    private Double reportedGrowth;
    private Double marketplaceRevenue;
    private Double revenueGrowth;
}
