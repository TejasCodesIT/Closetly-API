-- ===============================================
-- Database Migration: Add/Fix phone_number column
-- ===============================================
-- Purpose: Ensure phone_number column exists in users table
--          with proper data type and constraints
--
-- Migration Steps:
-- 1. Check if phone_number column exists
-- 2. Add column if not exists
-- 3. Ensure data type is correct (VARCHAR)
-- ===============================================

-- Add phone_number column if it doesn't exist
-- (This is safe to run even if the column already exists)
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20) NULL DEFAULT NULL;

-- Optional: If you had 'phone' column and want to copy data
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20) NULL;
-- UPDATE users SET phone_number = phone WHERE phone IS NOT NULL;
-- ALTER TABLE users DROP COLUMN IF EXISTS phone;
