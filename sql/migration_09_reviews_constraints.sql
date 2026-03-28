-- Migration 09 — Reviews: CHECK rating, UNIQUE (user, order, món) thay cho UNIQUE (user, order)
-- InnoDB: mỗi FK cần chỉ mục mà cột FK là tiền tố trái nhất. uk_reviews_user_order (user_id, order_id)
-- đang “gánh” fk_reviews_user — phải có idx_reviews_user_id TRƯỚC khi DROP uk.
-- Nếu báo "Duplicate key name", xóa dòng ADD INDEX tương ứng (đã chạy một phần).

USE restaurant_db;

ALTER TABLE reviews ADD INDEX idx_reviews_user_id (user_id);
ALTER TABLE reviews ADD INDEX idx_reviews_order_id (order_id);
ALTER TABLE reviews ADD INDEX idx_reviews_menu_item_id (menu_item_id);

ALTER TABLE reviews DROP INDEX uk_reviews_user_order;

ALTER TABLE reviews
    ADD CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5);

ALTER TABLE reviews
    ADD UNIQUE KEY uk_reviews_user_order_dish (user_id, order_id, menu_item_id);
