# Closetly Admin Management APIs

## Overview
Complete admin management system with dashboard metrics, report moderation, review management, system logging, and CSV exports.

---

## 1. Dashboard Overview API

### Endpoint
```
GET /api/admin/dashboard/overview
```

### Authentication
- Required: `Authorization: Bearer <JWT_TOKEN>`
- Role: `ADMIN`

### Request
No parameters required.

### Response (200 OK)
```json
{
  "grossBookings": 156,
  "grossBookingsGrowth": 12.5,
  "activeResellers": 45,
  "resellerGrowth": 8.2,
  "reportedItems": 3,
  "reportedGrowth": 50.0,
  "marketplaceRevenue": 5250.00,
  "revenueGrowth": 15.3
}
```

### Field Descriptions
- `grossBookings`: Total bookings in current month
- `grossBookingsGrowth`: Percentage growth vs previous month
- `activeResellers`: Count of active resellers with products
- `resellerGrowth`: Percentage growth in resellers
- `reportedItems`: Count of approved reports
- `reportedGrowth`: Percentage growth in reports
- `marketplaceRevenue`: Total platform revenue (10% commission)
- `revenueGrowth`: Percentage growth in revenue

---

## 2. Reported Products API

### 2.1 Get All Reported Products

#### Endpoint
```
GET /api/admin/reported-products?page=0&size=10
```

#### Request Parameters
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 10 | Items per page |

#### Response (200 OK)
```json
{
  "content": [
    {
      "reportId": 1,
      "productId": 101,
      "productTitle": "Designer Dress",
      "productImage": "https://image.url/dress.jpg",
      "sellerUsername": "John Doe",
      "sellerEmail": "john@example.com",
      "reason": "Inappropriate content",
      "description": "Contains offensive images",
      "status": "PENDING",
      "reportedAt": "2026-03-04T10:30:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": [],
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 25,
  "totalPages": 3,
  "last": false,
  "number": 0,
  "size": 10,
  "numberOfElements": 10,
  "first": true,
  "empty": false
}
```

---

### 2.2 Approve Report

#### Endpoint
```
POST /api/admin/reports/{reportId}/approve
```

#### Request Body
```json
{
  "adminNotes": "Product violates community guidelines. Approved for removal."
}
```

#### Response (200 OK)
```json
"Report approved successfully"
```

#### Error Response (404 Not Found)
```json
{
  "status": 404,
  "message": "Report not found",
  "details": null,
  "timestamp": "2026-03-04T10:35:00"
}
```

---

### 2.3 Block Product

#### Endpoint
```
POST /api/admin/reports/{reportId}/block
```

#### Request Body
```json
{
  "adminNotes": "Product permanently blocked due to policy violation"
}
```

#### Response (200 OK)
```json
"Product blocked successfully"
```

---

### 2.4 Reject Report

#### Endpoint
```
POST /api/admin/reports/{reportId}/reject
```

#### Request Body
```json
{
  "adminNotes": "Report does not meet removal criteria"
}
```

#### Response (200 OK)
```json
"Report rejected successfully"
```

---

## 3. Review Moderation API

### 3.1 Get Flagged Reviews

#### Endpoint
```
GET /api/admin/reviews?status=flagged&page=0&size=10
```

#### Request Parameters
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| status | string | pending | Filter by status (pending, approved, rejected) |
| page | int | 0 | Page number |
| size | int | 10 | Items per page |

#### Response (200 OK)
```json
{
  "content": [
    {
      "reviewId": 5,
      "productId": 101,
      "productTitle": "Designer Dress",
      "reviewerId": 42,
      "reviewerUsername": "Jane Smith",
      "rating": 1,
      "comment": "Scam product, poor quality",
      "flagReason": "Offensive language",
      "flagStatus": "PENDING",
      "reviewedAt": "2026-03-02T14:20:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 5,
  "totalPages": 1
}
```

---

### 3.2 Delete Review

#### Endpoint
```
DELETE /api/admin/reviews/{reviewId}
```

#### Response (200 OK)
```json
"Review deleted successfully"
```

#### Error Response (404 Not Found)
```json
{
  "status": 404,
  "message": "Review not found",
  "timestamp": "2026-03-04T10:40:00"
}
```

---

### 3.3 Ban User

#### Endpoint
```
POST /api/admin/users/{userId}/ban
```

#### Request Body
```json
{
  "banReason": "Multiple policy violations and fraudulent activity",
  "banDetails": "Used bot to inflate product ratings"
}
```

#### Response (200 OK)
```json
"User banned successfully. Reason: Multiple policy violations and fraudulent activity"
```

---

## 4. System Logs API

### Endpoint
```
GET /api/admin/system-logs?limit=10
```

### Request Parameters
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| limit | int | 10 | Number of logs to retrieve |

### Response (200 OK)
```json
{
  "content": [
    {
      "id": 1,
      "type": "REPORT_APPROVED",
      "message": "Report 1 approved. Product 101 blocked.",
      "details": null,
      "createdAt": "2026-03-04T10:30:00"
    },
    {
      "id": 2,
      "type": "USER_BANNED",
      "message": "User 42 (user@example.com) banned. Reason: Fraudulent activity",
      "details": null,
      "createdAt": "2026-03-04T10:25:00"
    },
    {
      "id": 3,
      "type": "REVIEW_DELETED",
      "message": "Review 5 deleted by admin.",
      "details": null,
      "createdAt": "2026-03-04T10:20:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 147
  }
}
```

### Log Types
- `REPORT_SUBMITTED` - New report submitted
- `REPORT_APPROVED` - Report approved by admin
- `REPORT_REJECTED` - Report rejected by admin
- `PRODUCT_BLOCKED` - Product blocked
- `USER_BANNED` - User account banned
- `REVIEW_DELETED` - Review deleted
- `SYSTEM_ACTION` - General system action

---

## 5. Export Report API

### Endpoint
```
GET /api/admin/reports/export?type=monthly
```

### Request Parameters
| Parameter | Type | Default | Options |
|-----------|------|---------|---------|
| type | string | monthly | monthly, yearly, weekly, all |

### Response (200 OK)
Headers:
```
Content-Type: text/plain
Content-Disposition: attachment; filename="reports_monthly_20260304_103000.csv"
```

Body (CSV Format):
```csv
Report ID,Product ID,Product Title,Seller Username,Reason,Status,Reported At,Approved At,Admin Notes
1,101,"Designer Dress","John Doe","Inappropriate content",APPROVED,2026-03-01T10:30:00,2026-03-04T10:30:00,"Product violates community guidelines"
2,102,"Vintage Jacket","Jane Smith","Counterfeit",PENDING,2026-03-03T14:15:00,,""
3,103,"Summer Shoes","Bob Johnson","Stolen item",APPROVED,2026-03-02T09:45:00,2026-03-03T11:20:00,"Report approved and product removed"
```

---

## Error Handling

All endpoints return standardized error responses using GlobalExceptionHandler:

### Bad Request (400)
```json
{
  "status": 400,
  "message": "Validation failed",
  "details": {
    "adminNotes": "Admin notes required"
  },
  "timestamp": "2026-03-04T10:35:00"
}
```

### Unauthorized (401)
```json
{
  "status": 401,
  "message": "Unauthorized access",
  "details": null,
  "timestamp": "2026-03-04T10:35:00"
}
```

### Forbidden (403)
```json
{
  "status": 403,
  "message": "Access denied. ADMIN role required",
  "details": null,
  "timestamp": "2026-03-04T10:35:00"
}
```

### Not Found (404)
```json
{
  "status": 404,
  "message": "Report not found",
  "details": null,
  "timestamp": "2026-03-04T10:35:00"
}
```

### Internal Server Error (500)
```json
{
  "status": 500,
  "message": "Error generating report: {error message}",
  "details": null,
  "timestamp": "2026-03-04T10:35:00"
}
```

---

## Authentication

All admin endpoints require JWT token with ADMIN role. Include in request header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### JWT Claims
The token must contain:
```json
{
  "sub": "user@example.com",
  "roles": ["ADMIN"],
  "iat": 1646380200,
  "exp": 1646466600
}
```

---

## Rate Limiting & Throttling

- No explicit rate limiting implemented (can be added)
- Pagination recommended for list endpoints
- Maximum 1000 items per export request

---

## Database Optimization

All queries use:
- **JPQL** for complex domain-specific queries
- **Native SQL** for performance-critical aggregations
- **Proper indexing** on frequently queried columns
- **Eager loading** where appropriate to avoid N+1 queries

Example optimized query in ReportRepository:
```
SELECT COUNT(DISTINCT r.product.id) FROM Report r 
WHERE r.status = 'APPROVED' AND r.reportedAt >= :startDate
```

---

## Implementation Notes

1. **Dashboard Service**: Uses JPQL for flexible queries and EntityManager for dynamic calculations
2. **Report Service**: Maintains referential integrity and logs all actions
3. **Review Service**: Soft deletes for audit trail preservation
4. **System Logs**: Indexed by creation date and type for fast retrieval
5. **CSV Export**: Properly escapes special characters for data integrity

## Testing
Test with ADMIN user:
```bash
# 1. Login to get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password"}'

# 2. Use token to access admin endpoints
curl -X GET http://localhost:8080/api/admin/dashboard/overview \
  -H "Authorization: Bearer <TOKEN>"
```
