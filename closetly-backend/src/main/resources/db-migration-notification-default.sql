-- ===============================================
-- Database Migration: Notification is_read Default Value
-- ===============================================
-- Purpose: Add DEFAULT FALSE to notifications.is_read column
--          This prevents MySQL errors when inserting notifications
--          without explicitly setting the read status
--
-- Migration Steps:
-- 1. Alter the is_read column to add DEFAULT FALSE
-- ===============================================

-- Add default value to is_read column
ALTER TABLE notifications MODIFY COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE;