# Closetly Admin Management System - Implementation Guide

## Table of Contents
1. [Setup & Configuration](#setup--configuration)
2. [Database Schema](#database-schema)
3. [Architecture Overview](#architecture-overview)
4. [API Endpoints Summary](#api-endpoints-summary)
5. [Running the Application](#running-the-application)
6. [Security Configuration](#security-configuration)
7. [Testing](#testing)
8. [Performance Optimization](#performance-optimization)

---

## Setup & Configuration

### Prerequisites
- Java 21
- Spring Boot 3.5.11
- MySQL 8.0+
- Maven 3.8+

### Installation Steps

#### 1. Database Migration
Execute the SQL schema in your MySQL database:

```bash
mysql -u root -p closetly_db < src/main/resources/sql/admin-schema.sql
```

Or manually run the SQL commands from `admin-schema.sql` file.

#### 2. Verify Entities
All required entities have been created:
- `Report.java` - Handles product reports
- `SystemLog.java` - Tracks admin actions
- `ReviewFlag.java` - Tracks flagged reviews

#### 3. Update Application Properties
Ensure these properties are in `application.properties`:

```properties
# Database Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# JWT Configuration (existing)
jwt.secret=your_jwt_secret_key
jwt.expiration=86400000

# Server Configuration
server.port=8080
server.servlet.context-path=/

# Logging
logging.level.root=INFO
logging.level.com.closetly=DEBUG
```

#### 4. Build and Deploy
```bash
mvn clean install
mvn spring-boot:run
```

---

## Database Schema

### Tables Created

#### 1. `reports` Table
```sql
CREATE TABLE reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'BLOCKED'),
    admin_notes VARCHAR(2000),
    reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (reporter_id) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_reported_at (reported_at),
    INDEX idx_product_id (product_id)
);
```

#### 2. `system_logs` Table
```sql
CREATE TABLE system_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type ENUM(...),
    message VARCHAR(2000) NOT NULL,
    details VARCHAR(5000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_created_at (created_at DESC),
    INDEX idx_type (type)
);
```

#### 3. `review_flags` Table
```sql
CREATE TABLE review_flags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    flagger_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED'),
    flagged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES reviews(id),
    FOREIGN KEY (flagger_id) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_flagged_at (flagged_at)
);
```

---

## Architecture Overview

### Directory Structure
```
admin/
├── controller/
│   └── AdminController.java         # Main REST endpoint handler
├── service/
│   ├── DashboardService.java        # Dashboard metrics aggregation
│   ├── ReportService.java           # Report management
│   ├── ReviewModerationService.java # Review moderation
│   ├── SystemLogService.java        # System log retrieval
│   └── ReportExportService.java     # CSV export functionality
├── repository/
│   ├── ReportRepository.java        # Report data access
│   ├── SystemLogRepository.java     # System log data access
│   └── ReviewFlagRepository.java    # Review flag data access
├── entity/
│   ├── Report.java                  # Report domain model
│   ├── SystemLog.java               # System log domain model
│   └── ReviewFlag.java              # Review flag domain model
├── dto/
│   ├── DashboardOverviewDTO.java
│   ├── ReportedProductDTO.java
│   ├── ReviewModerationDTO.java
│   ├── SystemLogDTO.java
│   ├── ReportExportDTO.java
│   ├── ReportActionDTO.java
│   └── UserBanDTO.java
└── mapper/
    └── AdminMapper.java             # DTO conversion utility
```

### Design Patterns Used

#### 1. **Service Layer Pattern**
- Separates business logic from HTTP concerns
- Enables reusability and testability
- Each service handles a specific domain

#### 2. **Repository Pattern**
- Abstracts data access logic
- Enables switching database implementations
- Provides query optimization point

#### 3. **DTO Pattern**
- Decouples API contracts from domain entities
- Enables flexible response formats
- Improves performance by selecting only needed fields

#### 4. **Mapper Pattern**
- Centralizes DTO conversion logic
- Maintains single responsibility principle
- Simplifies testing

---

## API Endpoints Summary

### Authentication Required
All `/api/admin/**` endpoints require:
- Valid JWT token in `Authorization: Bearer <TOKEN>` header
- User must have `ADMIN` role

### Dashboard
```
GET /api/admin/dashboard/overview
Returns: DashboardOverviewDTO with metrics
```

### Reports Management
```
GET /api/admin/reported-products?page=0&size=10
Returns: Paginated ReportedProductDTO list

POST /api/admin/reports/{reportId}/approve
Body: ReportActionDTO

POST /api/admin/reports/{reportId}/block
Body: ReportActionDTO

POST /api/admin/reports/{reportId}/reject
Body: ReportActionDTO
```

### Review Moderation
```
GET /api/admin/reviews?status=flagged&page=0&size=10
Returns: Paginated ReviewModerationDTO list

DELETE /api/admin/reviews/{reviewId}

POST /api/admin/users/{userId}/ban
Body: UserBanDTO
```

### System Logs
```
GET /api/admin/system-logs?limit=10
Returns: Paginated SystemLogDTO list
```

### Reports Export
```
GET /api/admin/reports/export?type=monthly
Returns: CSV file (monthly|yearly|weekly|all)
```

---

## Running the Application

### 1. Start the Spring Boot Application
```bash
cd closetly-backend
mvn spring-boot:run
```

### 2. Create Admin User (if not exists)
```sql
-- Insert admin role
INSERT INTO roles (name) VALUES ('ADMIN');

-- Create admin user
INSERT INTO users (email, password, full_name, enabled)
VALUES ('admin@closetly.com', '$2a$10$...encrypted_password...', 'Admin User', true);

-- Assign admin role to user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r 
WHERE u.email = 'admin@closetly.com' AND r.name = 'ADMIN';
```

### 3. Generate JWT Token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@closetly.com",
    "password": "admin_password"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "user": {
    "id": 1,
    "email": "admin@closetly.com",
    "fullName": "Admin User",
    "roles": ["ADMIN"]
  }
}
```

### 4. Test Admin Endpoints
```bash
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Get dashboard overview
curl -X GET http://localhost:8080/api/admin/dashboard/overview \
  -H "Authorization: Bearer $TOKEN"

# Get reported products
curl -X GET 'http://localhost:8080/api/admin/reported-products?page=0&size=10' \
  -H "Authorization: Bearer $TOKEN"
```

---

## Security Configuration

### JWT Token Configuration
The SecurityConfig has been updated to:
1. **Require authentication** for all `/api/admin/**` endpoints
2. **Allow public access** to public endpoints
3. **Use stateless sessions** with JWT
4. **Enable method-level security** with `@PreAuthorize`

### Controller-Level Security
```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")  // All methods require ADMIN role
public class AdminController { ... }
```

### Role Verification
The system verifies user roles at multiple levels:
1. **HTTP Security Filter** - Requires authentication for `/api/admin/**`
2. **@PreAuthorize Annotation** - Requires `ADMIN` role on controller
3. **Business Logic** - Entity relationships and soft deletes

---

## Testing

### Unit Tests Example
```java
@SpringBootTest
class AdminControllerTests {
    
    @MockBean
    private DashboardService dashboardService;
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testGetDashboardOverview_Authenticated() throws Exception {
        // Setup mock token
        String token = createMockJWT("ADMIN");
        
        DashboardOverviewDTO mockData = DashboardOverviewDTO.builder()
            .grossBookings(150L)
            .grossBookingsGrowth(12.5)
            .build();
        
        when(dashboardService.getOverview()).thenReturn(mockData);
        
        mockMvc.perform(get("/api/admin/dashboard/overview")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossBookings").value(150));
    }
    
    @Test
    void testGetDashboardOverview_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/overview"))
                .andExpect(status().isUnauthorized());
    }
}
```

### Integration Tests Example
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminIntegrationTests {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ReportRepository reportRepository;
    
    @Test
    void testApproveReport_ValidRequest() {
        // Create test report
        Report report = createTestReport();
        reportRepository.save(report);
        
        // Prepare request
        ReportActionDTO actionDTO = ReportActionDTO.builder()
            .adminNotes("Approved for removal")
            .build();
        
        // Execute request with admin token
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/admin/reports/{id}/approve",
            actionDTO,
            String.class,
            report.getId()
        );
        
        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify database state
        Report updated = reportRepository.findById(report.getId()).orElseThrow();
        assertEquals(Report.ReportStatus.APPROVED, updated.getStatus());
    }
}
```

### Manual Testing with cURL

#### 1. Get Dashboard Overview
```bash
curl -X GET http://localhost:8080/api/admin/dashboard/overview \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

#### 2. List Reported Products
```bash
curl -X GET 'http://localhost:8080/api/admin/reported-products?page=0&size=5' \
  -H "Authorization: Bearer $TOKEN"
```

#### 3. Approve a Report
```bash
curl -X POST http://localhost:8080/api/admin/reports/1/approve \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "adminNotes": "Product violates community guidelines"
  }'
```

#### 4. Delete a Review
```bash
curl -X DELETE http://localhost:8080/api/admin/reviews/5 \
  -H "Authorization: Bearer $TOKEN"
```

#### 5. Ban a User
```bash
curl -X POST http://localhost:8080/api/admin/users/42/ban \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "banReason": "Fraudulent activity",
    "banDetails": "Multiple policy violations"
  }'
```

#### 6. Export Reports
```bash
curl -X GET 'http://localhost:8080/api/admin/reports/export?type=monthly' \
  -H "Authorization: Bearer $TOKEN" \
  -o reports_monthly.csv
```

---

## Performance Optimization

### 1. Database Indexing
All tables have indexes on frequently queried columns:
- `reports.status` - For filtering pending reports
- `reports.reported_at` - For date range queries
- `system_logs.created_at` - For log retrieval
- `review_flags.status` - For status filtering

### 2. Query Optimization
- **JPQL Queries**: Used for flexible, type-safe queries with proper eager/lazy loading
- **Native SQL**: Used only for complex aggregations where performance is critical
- **Pagination**: Implemented on all list endpoints to limit data transfer

### 3. Caching Strategy (Optional)
```java
@Service
@CacheConfig(cacheNames = "dashboard")
public class DashboardService {
    
    @Cacheable(key = "#root.methodName")
    public DashboardOverviewDTO getOverview() {
        // Expensive calculation
    }
    
    @CacheEvict(key = "#root.methodName", allEntries = true)
    @Transactional
    public void approveReport(Long reportId, String adminNotes) {
        // Cache is cleared after report approval
    }
}
```

### 4. Connection Pooling
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

---

## Troubleshooting

### Issue: 401 Unauthorized on Admin Endpoints
**Solution**: 
1. Ensure JWT token is valid and has not expired
2. Verify user has ADMIN role: `SELECT * FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = ?`
3. Check JWT token claims decode to ensure `roles` contain `ADMIN`

### Issue: 403 Forbidden
**Solution**:
1. User is authenticated but doesn't have ADMIN role
2. Verify SecurityConfig allows unauthenticated access to public endpoints
3. Check `@PreAuthorize` annotation on controller

### Issue: Database Connection Errors
**Solution**:
1. Verify MySQL is running: `mysql -u root -p -e "SELECT 1"`
2. Check database connection string in `application.properties`
3. Ensure tables exist: `SHOW TABLES LIKE 'reports'`

### Issue: CSV Export is Empty
**Solution**:
1. Verify reports exist in database
2. Check date range filtering logic in `ReportExportService`
3. Ensure `reportedAt` timestamps are within the specified period

---

## Future Enhancements

1. **Role-Based Access Control (RBAC)**
   - Create fine-grained roles (MODERATOR, ANALYST, AUDITOR)
   - Implement role-specific endpoint access

2. **Advanced Analytics**
   - Add time-series data
   - Implement trend analysis
   - Create custom dashboards

3. **Audit Trail**
   - Log all admin actions with user info
   - Implement data change tracking
   - Add export audit logs feature

4. **Notification System**
   - Email notifications for appealed reports
   - SMS alerts for critical actions
   - In-app notifications

5. **Performance Monitoring**
   - Add Micrometer metrics
   - Implement custom health checks
   - Create performance dashboards

---

## Support & Documentation

- API Documentation: See [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
- SQL Queries: See [admin-schema.sql](./sql/admin-schema.sql)
- For issues: Create an issue in the project repository
