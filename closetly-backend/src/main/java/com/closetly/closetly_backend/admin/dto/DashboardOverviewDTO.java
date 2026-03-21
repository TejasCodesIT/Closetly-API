package com.closetly.closetly_backend.admin.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDTO {
    private Long grossBookings;
    private Long activeResellers;
    private Long totalUsers;
    private Long pendingReviews;
    private Long unresolvedReports;
    private Integer systemHealth;
    
}
