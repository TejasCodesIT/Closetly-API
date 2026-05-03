-- ===============================================
-- Database Migration: Fix Bookings Status Column Length
-- ===============================================
-- Purpose: Increase the length of the status column in bookings table
--          to accommodate longer enum values like 'CANCELLED_BY_CUSTOMER'
--
-- Issue: The status column was created with insufficient length to hold
--        enum values like 'CANCELLED_BY_CUSTOMER' (20 chars) and
--        'CANCELLED_BY_SELLER' (18 chars)
--
-- Solution: Alter the column to VARCHAR(30) to provide sufficient space
-- ===============================================

ALTER TABLE bookings MODIFY COLUMN status VARCHAR(30) NOT NULL;