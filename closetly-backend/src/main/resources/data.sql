-- Insert default roles
INSERT INTO roles (name) VALUES ('USER') ON DUPLICATE KEY UPDATE name = name;
INSERT INTO roles (name) VALUES ('ADMIN') ON DUPLICATE KEY UPDATE name = name;

-- Insert sample user
INSERT INTO users (email, password, full_name, phone, created_at, updated_at) VALUES
('test@example.com', '$2a$10$examplehashedpassword', 'Test User', '1234567890', NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Insert sample product
INSERT INTO products (title, description, brand, category, size, product_condition, sale_price, rent_price_per_day, is_for_sale, is_for_rent, quantity, seller_id, status, created_at, updated_at, deleted, popularity) VALUES
('Sample Dress', 'A beautiful sample dress for testing', 'Zara', 'Dresses', 'M', 'NEW', 50.00, 5.00, true, true, 10, (SELECT id FROM users WHERE email = 'test@example.com' LIMIT 1), 'ACTIVE', NOW(), NOW(), false, 100)
ON DUPLICATE KEY UPDATE title = title;