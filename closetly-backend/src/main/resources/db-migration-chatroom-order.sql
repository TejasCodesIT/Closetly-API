-- ===============================================
-- Database Migration: Chat Room Order Support
-- ===============================================
-- Problem: chat_rooms.chat_id was NOT NULL on the table; order-based chat inserts set booking_id = NULL.
-- Solution: allow booking_id NULL, add optional order_id FK, keep existing booking behavior and support order chats.

-- Step 1: Allow booking_id to be nullable.
ALTER TABLE chat_rooms MODIFY COLUMN booking_id BIGINT NULL;

-- Step 2: Add order_id (optional) to support order-based chats.
ALTER TABLE chat_rooms
  ADD COLUMN IF NOT EXISTS order_id BIGINT NULL;

-- Step 3: Add foreign key constraint for order -> orders.id, optional.
ALTER TABLE chat_rooms
  ADD CONSTRAINT IF NOT EXISTS FK_chat_rooms_order_id FOREIGN KEY (order_id) REFERENCES orders(id);

-- Step 4: Drop existing booking/product uniqueness because we'll enforce via deleted-aware composite keys.
ALTER TABLE chat_rooms
  DROP INDEX IF EXISTS UKll6oaol6krvgujbv0adc8ub73,
  DROP INDEX IF EXISTS uq_chat_rooms_product_buyer;

-- Step 5: Add deleted-aware unique constraints for chat dedupe semantics.
ALTER TABLE chat_rooms
  ADD UNIQUE INDEX uq_chat_rooms_booking_deleted (booking_id, deleted),
  ADD UNIQUE INDEX uq_chat_rooms_order_deleted (order_id, deleted),
  ADD UNIQUE INDEX uq_chat_rooms_product_buyer_deleted (product_id, buyer_id, deleted);

-- Step 6: (Optional) if not exists (unsupported with IF NOT EXISTS prior to MySQL 8) add order index.
ALTER TABLE chat_rooms
  ADD INDEX idx_chat_rooms_order_id (order_id);

-- Verification queries:
-- DESCRIBE chat_rooms;
-- SELECT * FROM information_schema.COLUMNS WHERE table_name='chat_rooms' AND column_name IN('booking_id', 'order_id');

