-- Migration 07 — Quản lý user: cột users.is_active
-- Toàn bộ schema đầy đủ đã có trong sql/schema.sql.
-- Chỉ chạy file này khi bạn đang nâng cấp database CŨ được tạo trước khi có cột is_active.
-- Nếu báo lỗi "Duplicate column name" thì bỏ qua (cột đã tồn tại).

USE restaurant_db;

ALTER TABLE users
    ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1;
