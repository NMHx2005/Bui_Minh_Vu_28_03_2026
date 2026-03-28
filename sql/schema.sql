-- Restaurant Management — Buổi 1
-- Chạy: mysql -u root -p < sql/schema.sql

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

-- Bàn ăn (tên bảng `dining_tables` tránh nhầm với từ khóa `tables`)
CREATE TABLE dining_tables (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    table_code      VARCHAR(50) NOT NULL,
    capacity        INT UNSIGNED NOT NULL,
    status          ENUM('FREE', 'OCCUPIED') NOT NULL DEFAULT 'FREE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dining_tables_code (table_code),
    KEY idx_dining_tables_status (status)
) ENGINE=InnoDB;

-- Thực đơn
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

-- Hóa đơn / phiên order
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
    KEY idx_orders_status (status)
) ENGINE=InnoDB;

-- Chi tiết từng món trong order
CREATE TABLE order_details (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT UNSIGNED NOT NULL,
    menu_item_id    BIGINT UNSIGNED NOT NULL,
    quantity        INT UNSIGNED NOT NULL,
    unit_price      DECIMAL(12, 2) NOT NULL,
    line_status     ENUM('PENDING', 'COOKING', 'READY', 'SERVED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_od_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_od_menu FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    KEY idx_od_order (order_id),
    KEY idx_od_line_status (line_status),
    KEY idx_od_created (created_at)
) ENGINE=InnoDB;

-- Seed: manager (mật khẩu: Manager@123) — hash bcrypt sinh bằng jBCrypt
INSERT INTO users (username, password_hash, role, is_active) VALUES
('manager', '$2a$10$dyVNUsvxk/plJX6qxcJ26ujd9eCXZ7zrmIxskD7tnnmzBvdfCjpIG', 'MANAGER', 1),
('chef1', '$2a$10$dyVNUsvxk/plJX6qxcJ26ujd9eCXZ7zrmIxskD7tnnmzBvdfCjpIG', 'CHEF', 1);

INSERT INTO dining_tables (table_code, capacity, status) VALUES
('T01', 4, 'FREE'),
('T02', 2, 'FREE'),
('T03', 6, 'FREE');

INSERT INTO menu_items (name, item_type, price, stock_quantity, is_active) VALUES
('Phở bò', 'FOOD', 65000.00, NULL, 1),
('Cơm rang', 'FOOD', 55000.00, NULL, 1),
('Nước suối', 'DRINK', 10000.00, 100, 1),
('Trà đá', 'DRINK', 5000.00, 200, 1);
