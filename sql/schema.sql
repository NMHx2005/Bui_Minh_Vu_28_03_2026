-- Restaurant Management — Buổi 1 + seed demo (Buổi 2–5 & nâng cao)
-- Chạy: mysql -u root -p < sql/schema.sql
-- DB cũ: DROP DATABASE IF EXISTS restaurant_db; trước khi import lại.

CREATE DATABASE IF NOT EXISTS restaurant_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE restaurant_db;

-- Người dùng
CREATE TABLE users (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            ENUM('MANAGER', 'CHEF', 'CUSTOMER') NOT NULL,
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_username (username),
    KEY idx_users_role (role)
) ENGINE=InnoDB;

CREATE TABLE dining_tables (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    table_code      VARCHAR(50) NOT NULL,
    capacity        INT UNSIGNED NOT NULL,
    status          ENUM('FREE', 'OCCUPIED') NOT NULL DEFAULT 'FREE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dining_tables_code (table_code),
    KEY idx_dining_tables_status (status)
) ENGINE=InnoDB;

CREATE TABLE menu_items (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    item_type       ENUM('FOOD', 'DRINK') NOT NULL,
    price           DECIMAL(12, 2) NOT NULL,
    stock_quantity  INT NULL COMMENT 'NULL = không áp dụng (đồ ăn); số nguyên >= 0 cho đồ uống',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_menu_items_type (item_type),
    KEY idx_menu_items_name (name)
) ENGINE=InnoDB;

CREATE TABLE orders (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    table_id            BIGINT UNSIGNED NOT NULL,
    customer_user_id    BIGINT UNSIGNED NULL,
    status              ENUM('OPEN', 'PAID', 'CANCELLED') NOT NULL DEFAULT 'OPEN',
    total_amount        DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    checked_out_at      DATETIME NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_table FOREIGN KEY (table_id) REFERENCES dining_tables (id),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_user_id) REFERENCES users (id),
    KEY idx_orders_table (table_id),
    KEY idx_orders_customer (customer_user_id),
    KEY idx_orders_status (status),
    KEY idx_orders_checked_out (checked_out_at)
) ENGINE=InnoDB;

-- manager_approval: phục vụ nâng cao “Duyệt món” (08). Mặc định APPROVED = luồng hiện tại không cần đổi code ngay.
CREATE TABLE order_details (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id            BIGINT UNSIGNED NOT NULL,
    menu_item_id        BIGINT UNSIGNED NOT NULL,
    quantity            INT UNSIGNED NOT NULL,
    unit_price          DECIMAL(12, 2) NOT NULL,
    line_status         ENUM('PENDING', 'COOKING', 'READY', 'SERVED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    manager_approval    ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'APPROVED',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_od_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_od_menu FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    KEY idx_od_order (order_id),
    KEY idx_od_line_status (line_status),
    KEY idx_od_created (created_at),
    KEY idx_od_manager_approval (manager_approval)
) ENGINE=InnoDB;

-- Nâng cao đánh giá (09)
CREATE TABLE reviews (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED NOT NULL,
    order_id        BIGINT UNSIGNED NULL,
    menu_item_id    BIGINT UNSIGNED NULL,
    rating          TINYINT UNSIGNED NOT NULL,
    comment         TEXT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE SET NULL,
    CONSTRAINT fk_reviews_menu FOREIGN KEY (menu_item_id) REFERENCES menu_items (id) ON DELETE SET NULL,
    UNIQUE KEY uk_reviews_user_order (user_id, order_id),
    KEY idx_reviews_created (created_at)
) ENGINE=InnoDB;

-- =============================================================================
-- Seed — mật khẩu ứng dụng: Manager@123 (bcrypt dưới đây), trừ user_bi_khoa (inactive)
-- $2a$10$dyVNUsvxk/plJX6qxcJ26ujd9eCXZ7zrmIxskD7tnnmzBvdfCjpIG
-- =============================================================================
-- id: 1 manager, 2 chef1, 3 chef2, 4 customer1, 5 customer2, 6 customer_demo,
--     7 user_bi_khoa, 8 manager_phu (2 manager active — test vô hiệu hóa 07)

INSERT INTO users (username, password_hash, role, is_active) VALUES
('manager', '$2a$10$dyVNUsvxk/plJX6qxcJ26ujd9eCXZ7zrmIxskD7tnnmzBvdfCjpIG', 'MANAGER', 1),
('chef1', '$2a$10$dyVNUsvxk/plJX6qxcJ26ujd9eCXZ7zrmIxskD7tnnmzBvdfCjpIG', 'CHEF', 1),
('chef2', '$2a$10$dyVNUsvxk/plJX6qxcJ26ujd9eCXZ7zrmIxskD7tnnmzBvdfCjpIG', 'CHEF', 1),
('customer1', '$2a$10$dyVNUsvxk/plJX6qxcJ26ujd9eCXZ7zrmIxskD7tnnmzBvdfCjpIG', 'CUSTOMER', 1),
('customer2', '$2a$10$dyVNUsvxk/plJX6qxcJ26ujd9eCXZ7zrmIxskD7tnnmzBvdfCjpIG', 'CUSTOMER', 1),
('customer_demo', '$2a$10$dyVNUsvxk/plJX6qxcJ26ujd9eCXZ7zrmIxskD7tnnmzBvdfCjpIG', 'CUSTOMER', 1),
('user_bi_khoa', '$2a$10$dyVNUsvxk/plJX6qxcJ26ujd9eCXZ7zrmIxskD7tnnmzBvdfCjpIG', 'CUSTOMER', 0),
('manager_phu', '$2a$10$dyVNUsvxk/plJX6qxcJ26ujd9eCXZ7zrmIxskD7tnnmzBvdfCjpIG', 'MANAGER', 1);

INSERT INTO dining_tables (table_code, capacity, status) VALUES
('T01', 4, 'FREE'),
('T02', 2, 'FREE'),
('T03', 6, 'FREE'),
('T04', 4, 'FREE'),
('T05', 8, 'FREE'),
('VIP01', 10, 'FREE'),
('BAR02', 2, 'FREE'),
('T08', 8, 'FREE'),
('T09', 4, 'FREE'),
('T10', 4, 'FREE');

INSERT INTO menu_items (name, item_type, price, stock_quantity, is_active) VALUES
('Phở bò', 'FOOD', 65000.00, NULL, 1),
('Cơm rang dưa bò', 'FOOD', 55000.00, NULL, 1),
('Nước suối', 'DRINK', 10000.00, 100, 1),
('Trà đá', 'DRINK', 5000.00, 200, 1),
('Bún chả Hà Nội', 'FOOD', 60000.00, NULL, 1),
('Gỏi cuốn tôm thịt', 'FOOD', 45000.00, NULL, 1),
('Bánh mì thịt nướng', 'FOOD', 35000.00, NULL, 1),
('Coca-Cola', 'DRINK', 15000.00, 80, 1),
('Cà phê đá', 'DRINK', 18000.00, 50, 1),
('Trà chanh', 'DRINK', 12000.00, 60, 1),
('Món ngừng kinh doanh (test)', 'FOOD', 1.00, NULL, 0);

-- --- Orders: 1–3 PAID (thanh toán + thống kê 06/10), 4–5 OPEN (bếp 04 + khách 03) ---
INSERT INTO orders (table_id, customer_user_id, status, total_amount, checked_out_at) VALUES
(1, 4, 'PAID', 145000.00, '2026-03-14 20:15:00'),
(2, 6, 'PAID', 84000.00, '2026-03-20 12:30:00'),
(5, 4, 'PAID', 205000.00, '2026-03-26 19:45:00'),
(8, 5, 'OPEN', 130000.00, NULL),
(9, 6, 'OPEN', 240000.00, NULL);

-- Order 1 PAID — đã thanh toán, bàn T01 hiện FREE
INSERT INTO order_details (order_id, menu_item_id, quantity, unit_price, line_status, manager_approval) VALUES
(1, 1, 2, 65000.00, 'SERVED', 'APPROVED'),
(1, 8, 1, 15000.00, 'SERVED', 'APPROVED');

-- Order 2 PAID
INSERT INTO order_details (order_id, menu_item_id, quantity, unit_price, line_status, manager_approval) VALUES
(2, 5, 1, 60000.00, 'SERVED', 'APPROVED'),
(2, 10, 2, 12000.00, 'SERVED', 'APPROVED');

-- Order 3 PAID — nhiều món (top món / doanh thu)
INSERT INTO order_details (order_id, menu_item_id, quantity, unit_price, line_status, manager_approval) VALUES
(3, 1, 1, 65000.00, 'SERVED', 'APPROVED'),
(3, 2, 2, 55000.00, 'SERVED', 'APPROVED'),
(3, 3, 3, 10000.00, 'SERVED', 'APPROVED');

-- Order 4 OPEN — customer2 @ T08 (Buổi 3 + 04)
INSERT INTO order_details (order_id, menu_item_id, quantity, unit_price, line_status, manager_approval) VALUES
(4, 1, 1, 65000.00, 'PENDING', 'APPROVED'),
(4, 4, 2, 5000.00, 'PENDING', 'APPROVED'),
(4, 2, 1, 55000.00, 'COOKING', 'APPROVED');

-- Order 5 OPEN — customer_demo @ T09: đủ trạng thái bếp + 1 dòng chờ duyệt manager (08)
INSERT INTO order_details (order_id, menu_item_id, quantity, unit_price, line_status, manager_approval) VALUES
(5, 5, 1, 60000.00, 'PENDING', 'APPROVED'),
(5, 1, 1, 65000.00, 'COOKING', 'APPROVED'),
(5, 2, 1, 55000.00, 'READY', 'APPROVED'),
(5, 6, 1, 45000.00, 'SERVED', 'APPROVED'),
(5, 8, 1, 15000.00, 'PENDING', 'PENDING');

-- Order 6 CANCELLED — lịch sử (test / báo cáo)
INSERT INTO orders (table_id, customer_user_id, status, total_amount, checked_out_at) VALUES
(3, 4, 'CANCELLED', 0.00, NULL);

INSERT INTO order_details (order_id, menu_item_id, quantity, unit_price, line_status, manager_approval) VALUES
(6, 7, 1, 35000.00, 'CANCELLED', 'REJECTED');

UPDATE dining_tables SET status = 'OCCUPIED' WHERE id IN (8, 9);

-- Tồn kho sau mọi giao dịch seed (đồ uống đã bán trong PAID + OPEN)
-- Ban đầu: suối 100, trà đá 200, coca 80, cafe 50, trà chanh 60
-- Trừ: od1 coca 1; od2 trà chanh 2; od3 suối 3; od4 trà đá 2; od5 coca 1 (PENDING chưa trừ thực tế app — seed chỉnh tay cho nhất quán demo)
UPDATE menu_items SET stock_quantity = 97 WHERE id = 3;
UPDATE menu_items SET stock_quantity = 198 WHERE id = 4;
-- Coca: −1 (order 1) −1 (order 5 PENDING trong seed demo)
UPDATE menu_items SET stock_quantity = 78 WHERE id = 8;
UPDATE menu_items SET stock_quantity = 58 WHERE id = 10;

-- Reviews (09) — gắn order PAID
INSERT INTO reviews (user_id, order_id, menu_item_id, rating, comment) VALUES
(4, 1, NULL, 5, 'Phục vụ nhanh, phở rất ngon.'),
(6, 2, NULL, 4, 'Ổn, trà chanh hơi ngọt.'),
(4, 3, 1, 5, 'Phở vẫn là món ruột.');
