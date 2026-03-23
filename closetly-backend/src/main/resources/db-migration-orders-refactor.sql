-- ===============================================
-- Database Migration: Orders Refactoring
-- ===============================================
-- Purpose: Remove product_id and price columns from orders table
--          These columns violate the design because:
--          1. An order can have multiple products (now handled by order_items)
--          2. Price information should be stored per item (in order_items.unit_price and total_price)
--          3. Single product_id doesn't support cart-based multi-item orders
--
-- Migration Steps:
-- 1. Drop old constraints referencing product_id
-- 2. Remove product_id and price columns from orders table
-- 3. Ensure order_items table is properly set up
-- ===============================================

-- Step 1: Remove Foreign Key Constraint (if exists)
ALTER TABLE orders DROP FOREIGN KEY orders_ibfk_2;

-- Step 2: Remove old columns
ALTER TABLE orders DROP COLUMN IF EXISTS product_id;
ALTER TABLE orders DROP COLUMN IF EXISTS price;

-- Step 3: Verify order_items table has all required columns
-- (This should already exist, but verify the schema)
-- Expected columns in order_items:
-- - id (PRIMARY KEY)
-- - order_id (FOREIGN KEY -> orders.id)
-- - product_id (FOREIGN KEY -> products.id)
-- - quantity (INT, NOT NULL)
-- - unit_price (DOUBLE, NOT NULL)
-- - total_price (DOUBLE, NOT NULL)
-- - type (VARCHAR(20), NOT NULL) - BUY or RENT
-- - start_date (DATE) - for rentals
-- - end_date (DATE) - for rentals
-- - rental_days (INT) - for rentals
-- - created_at (TIMESTAMP)

-- Step 4: Add missing columns to order_items if they don't exist
-- (Uncomment if needed)
-- ALTER TABLE order_items ADD COLUMN unit_price DOUBLE NOT NULL DEFAULT 0;
-- ALTER TABLE order_items ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'BUY';
-- ALTER TABLE order_items ADD COLUMN start_date DATE;
-- ALTER TABLE order_items ADD COLUMN end_date DATE;
-- ALTER TABLE order_items ADD COLUMN rental_days INT;

-- Step 5: Verify orders table structure after migration
-- Expected columns:
-- - id (PRIMARY KEY)
-- - customer_id (FOREIGN KEY -> users.id)
-- - total_amount (DOUBLE, NOT NULL)
-- - total_items (INT, NOT NULL)
-- - status (VARCHAR(50), NOT NULL)
-- - created_at (TIMESTAMP)
-- - deleted (BOOLEAN, DEFAULT 0)

-- ===============================================
-- Verification Queries
-- ===============================================
-- After migration, run these queries to verify:

-- Verify orders table schema:
-- DESCRIBE orders;

-- Verify order_items table schema:
-- DESCRIBE order_items;

-- Check any remaining orders with incomplete data:
-- SELECT o.id, o.customer_id, o.total_amount, o.total_items, COUNT(oi.id) as item_count
-- FROM orders o
-- LEFT JOIN order_items oi ON o.id = oi.order_id
-- GROUP BY o.id
-- HAVING item_count = 0;

-- ===============================================
-- Rollback Instructions (if needed)
-- ===============================================
-- If something goes wrong, this will NOT restore the old columns.
-- You would need to:
-- 1. Restore from backup
-- 2. Or manually add columns back and update the application code
