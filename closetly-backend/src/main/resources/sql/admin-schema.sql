-- =============================================
-- Closetly Admin Module - Database Schema
-- =============================================

-- Reports table
CREATE TABLE IF NOT EXISTS reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'BLOCKED') NOT NULL DEFAULT 'PENDING',
    admin_notes VARCHAR(2000),
    reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (reporter_id) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_reported_at (reported_at),
    INDEX idx_product_id (product_id)
);

-- System Logs table
CREATE TABLE IF NOT EXISTS system_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type ENUM('REPORT_SUBMITTED', 'REPORT_APPROVED', 'REPORT_REJECTED', 
              'PRODUCT_BLOCKED', 'USER_BANNED', 'REVIEW_DELETED', 'SYSTEM_ACTION') NOT NULL,
    message VARCHAR(2000) NOT NULL,
    details VARCHAR(5000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_created_at (created_at DESC),
    INDEX idx_type (type)
);

-- Review Flags table
CREATE TABLE IF NOT EXISTS review_flags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    flagger_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    flagged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES reviews(id),
    FOREIGN KEY (flagger_id) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_flagged_at (flagged_at),
    INDEX idx_review_id (review_id)
);

-- =============================================
-- Sample Queries
-- =============================================

-- Dashboard Overview Queries

-- 1. Gross Bookings for current month
SELECT COUNT(b.id) as grossBookings
FROM bookings b
WHERE YEAR(b.created_at) = YEAR(CURDATE())
AND MONTH(b.created_at) = MONTH(CURDATE());

-- 2. Gross Bookings for previous month
SELECT COUNT(b.id) as previousBookings
FROM bookings b
WHERE YEAR(b.created_at) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
AND MONTH(b.created_at) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH));

-- 3. Active Resellers (with active products)
SELECT COUNT(DISTINCT p.seller_id) as activeResellers
FROM products p
WHERE p.status = 'ACTIVE'
AND YEAR(p.created_at) = YEAR(CURDATE())
AND MONTH(p.created_at) = MONTH(CURDATE());

-- 4. Reported Items (approved reports)
SELECT COUNT(DISTINCT r.product_id) as reportedItems
FROM reports r
WHERE r.status = 'APPROVED'
AND YEAR(r.reported_at) = YEAR(CURDATE())
AND MONTH(r.reported_at) = MONTH(CURDATE());

-- 5. Marketplace Revenue (from bookings)
SELECT COALESCE(SUM(b.total_price) * 0.10, 0) as marketplaceRevenue
FROM bookings b
WHERE YEAR(b.created_at) = YEAR(CURDATE())
AND MONTH(b.created_at) = MONTH(CURDATE());

-- =============================================
-- Reporting Queries
-- =============================================

-- Get all reported products with details
SELECT 
    r.id as reportId,
    p.id as productId,
    p.title as productTitle,
    (SELECT url FROM product_images pi WHERE pi.product_id = p.id LIMIT 1) as productImage,
    u.full_name as sellerUsername,
    u.email as sellerEmail,
    r.reason,
    r.description,
    r.status,
    r.reported_at
FROM reports r
JOIN products p ON r.product_id = p.id
JOIN users u ON p.seller_id = u.id
WHERE r.status = 'PENDING'
ORDER BY r.reported_at DESC;

-- Get reports statistics by reason
SELECT 
    reason,
    COUNT(*) as count,
    COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) as approved,
    COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending,
    COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) as rejected
FROM reports
GROUP BY reason
ORDER BY count DESC;

-- =============================================
-- Review Moderation Queries
-- =============================================

-- Get all flagged reviews
SELECT 
    rf.id as flagId,
    r.id as reviewId,
    p.id as productId,
    p.title as productTitle,
    u.id as reviewerId,
    u.full_name as reviewerUsername,
    r.rating,
    r.comment,
    rf.reason as flagReason,
    rf.status,
    r.created_at as reviewedAt
FROM review_flags rf
JOIN reviews r ON rf.review_id = r.id
JOIN products p ON r.product_id = p.id
JOIN users u ON r.reviewer_id = u.id
WHERE rf.status = 'PENDING'
ORDER BY rf.flagged_at DESC;

-- =============================================
-- System Logs Queries
-- =============================================

-- Get recent system logs
SELECT 
    id,
    type,
    message,
    details,
    created_at
FROM system_logs
ORDER BY created_at DESC
LIMIT 100;

-- Get logs by type
SELECT 
    type,
    COUNT(*) as count,
    MAX(created_at) as lastOccurrence
FROM system_logs
GROUP BY type
ORDER BY count DESC;

-- =============================================
-- Export Queries
-- =============================================

-- Monthly Report Export
SELECT 
    r.id as reportId,
    p.id as productId,
    p.title as productTitle,
    u.full_name as sellerUsername,
    r.reason,
    r.status,
    r.reported_at,
    r.updated_at as approvedAt,
    r.admin_notes as adminNotes
FROM reports r
JOIN products p ON r.product_id = p.id
JOIN users u ON p.seller_id = u.id
WHERE YEAR(r.reported_at) = YEAR(CURDATE())
AND MONTH(r.reported_at) = MONTH(CURDATE())
ORDER BY r.reported_at DESC;

-- =============================================
-- Admin Action Logging
-- =============================================

-- Log when a report is approved
INSERT INTO system_logs (type, message, details)
VALUES (
    'REPORT_APPROVED',
    CONCAT('Report ', ?, ' approved'),
    CONCAT('Product ID: ', ?, ', Admin Notes: ', ?)
);

-- Log when a product is blocked
INSERT INTO system_logs (type, message, details)
VALUES (
    'PRODUCT_BLOCKED',
    CONCAT('Product ', ? , ' permanently blocked'),
    CONCAT('Report ID: ', ?, ', Reason: ', ?)
);

-- Log when a user is banned
INSERT INTO system_logs (type, message, details)
VALUES (
    'USER_BANNED',
    CONCAT('User ', ?, ' banned'),
    CONCAT('Email: ', ?, ', Reason: ', ?)
);

-- =============================================
-- Performance Optimization Indexes
-- =============================================

-- Already included in table definitions, but additional useful indexes:

-- For report searches
CREATE INDEX IF NOT EXISTS idx_reports_product_status ON reports(product_id, status);
CREATE INDEX IF NOT EXISTS idx_reports_reporter ON reports(reporter_id);

-- For review flags
CREATE INDEX IF NOT EXISTS idx_review_flags_status_date ON review_flags(status, flagged_at DESC);

-- For system logs (useful for real-time monitoring)
CREATE INDEX IF NOT EXISTS idx_system_logs_type_date ON system_logs(type, created_at DESC);
