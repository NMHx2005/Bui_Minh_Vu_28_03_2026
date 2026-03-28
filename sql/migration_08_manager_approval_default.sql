-- Migration 08 — Duyệt món: mặc định manager_approval = PENDING cho dòng mới
-- Toàn bộ DDL đã cập nhật trong sql/schema.sql. Chạy khi nâng cấp DB cũ (default còn APPROVED).

USE restaurant_db;

ALTER TABLE order_details
    MODIFY COLUMN manager_approval ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING';
