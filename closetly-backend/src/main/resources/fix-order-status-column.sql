-- ===============================================
-- Fix Order Status Column Data Truncation Error
-- ===============================================
-- Problem: "Data truncated for column 'status' at row 1"
-- Cause: Database column 'status' is not compatible with enum values
-- Solution: Change column to VARCHAR(50) to accommodate all enum values
-- ===============================================

-- Fix the orders table status column
ALTER TABLE orders MODIFY COLUMN status VARCHAR(50) NOT NULL;

-- Verify the change
-- DESCRIBE orders;

-- Check current status values in the table
-- SELECT DISTINCT status FROM orders;