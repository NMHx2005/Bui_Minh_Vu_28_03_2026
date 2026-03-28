# Hướng dẫn test và sử dụng — Restaurant Management (Console)

Tài liệu này mô tả cách chạy app, tài khoản mẫu và luồng thao tác theo từng vai trò để kiểm thử đủ chức năng.

---

## 1. Chuẩn bị nhanh

1. Tạo DB + seed: `bash sql/init-db.sh` (hoặc `mysql -u root -p < sql/schema.sql`).
2. File `src/main/resources/db.properties` (sao chép từ `db.properties.example`).
3. Chạy: `./mvnw -q compile exec:java`

Chi tiết cài đặt xem `README.md`.

---

## 2. Tài khoản mẫu (sau khi import `schema.sql`)

| Username        | Vai trò   | Ghi chú |
|-----------------|-----------|---------|
| `manager`       | Quản lý   | Mật khẩu: **Manager@123** |
| `manager_phu`   | Quản lý   | Thứ 2 manager — test không vô hiệu manager cuối |
| `chef1`, `chef2`| Đầu bếp   | Cùng mật khẩu **Manager@123** |
| `customer1`     | Khách     | Không có order OPEN — dùng để chọn bàn mới |
| `customer2`     | Khách     | Order **OPEN #4**, bàn **T08** |
| `customer_demo` | Khách     | Order **OPEN #5**, bàn **T09** (nhiều trạng thái bếp + Coca chờ duyệt) |
| `user_bi_khoa`  | Khách     | **Vô hiệu** — đăng nhập bị từ chối (test login) |

---

## 3. Menu chính (trước đăng nhập)

- **1** — Đăng nhập (tên `0` = quay lại).
- **2** — Đăng ký khách (username chữ/số/`_`, mật khẩu 6–128 ký tự).
- **0** — Thoát chương trình.

**Test nhanh:** đăng nhập `user_bi_khoa` / `Manager@123` → thấy thông báo tài khoản vô hiệu.

---

## 4. Khách hàng (`customer*`)

**Menu:** 1–7, **0** = đăng xuất.

| Mục | Chức năng | Gợi ý test |
|-----|-----------|------------|
| 1 | Xem thực đơn | Chỉ món `is_active` |
| 2 | Chọn bàn trống | `customer1`: chọn ID bàn **FREE** trong bảng |
| 3 | Gọi món | Cần đã có phiên OPEN; đồ uống chỉ **kiểm tra** tồn (trừ kho khi quản lý duyệt) |
| 4 | Theo dõi món | Cột **QL duyệt**, **Bếp** |
| 5 | Hủy món | Chỉ dòng **PENDING** bếp + **Chờ** duyệt QL |
| 6 | Thanh toán | Chỉ khi mọi dòng (trừ hủy) **SERVED**; in hóa đơn |
| 7 | Đánh giá | Chỉ order **PAID** của chính user; submenu 1–3, **0** quay lại |

**Luồng end-to-end gợi ý (customer1):**

1. Đăng nhập → 2 chọn bàn → 3 gọi món (FOOD + DRINK).
2. Đăng nhập **manager** → 5 duyệt đồ uống (nếu có PENDING) → đồ ăn READY thì duyệt.
3. Đăng **chef1** → 2 cập nhật từng dòng đến **SERVED** (đúng quy tắc FOOD/DRINK).
4. Đăng lại khách → 6 thanh toán.
5. 7 → đánh giá tổng thể / từng món.

**Với `customer2` / `customer_demo`:** dùng sẵn order OPEN để test 3–6 mà không cần chọn bàn (trừ khi muốn phiên mới).

---

## 5. Đầu bếp (`chef1` / `chef2`)

**Menu:** 1–2, **0** = đăng xuất.

| Mục | Chức năng |
|-----|-----------|
| 1 | Xem hàng đợi (cột Loại, QL duyệt) |
| 2 | Nhập **ID dòng** — chuyển **một bước** (FOOD: READY chưa duyệt QL thì không lên SERVED; DRINK chỉ sau khi QL duyệt) |

**Test:** đăng nhập `chef1`, mục 1 xem queue; mục 2 thử với ID dòng từ bảng (order #4 hoặc #5 sau khi manager duyệt đồ uống).

---

## 6. Quản lý (`manager`)

**Menu chính:** 1–7, **0** = đăng xuất.

| Mục | Nội dung | Ghi chú |
|-----|----------|---------|
| 1 | Thực đơn | Submenu 1–5: list / thêm / sửa / ẩn / tìm — **0** quay lại |
| 2 | Bàn | Submenu 1–5 — **0** quay lại |
| 3 | Thanh toán | Nhập order **OPEN** → xem dòng → Y/N → hóa đơn |
| 4 | Người dùng | List / tạo CHEF / vô hiệu (không tự ban; không ban manager cuối) |
| 5 | Duyệt món | Chọn order OPEN → duyệt/từ chối/batch — FOOD chỉ khi **READY**, DRINK kiểm tra kho + trừ khi duyệt |
| 6 | Đánh giá | Xem review khách |
| 7 | Thống kê | Doanh thu khoảng ngày / theo tháng; top món theo SL / doanh thu (ngày nhập `yyyy-MM-dd`) |

**Order mẫu trong seed:** OPEN **#4**, **#5**; PAID **#1–#3** (doanh thu thống kê — thử tháng **3/2026** hoặc khoảng **2026-03-01** → **2026-03-31**).

---

## 7. Checklist kiểm thử theo nhóm tính năng

- [ ] Đăng ký + đăng nhập khách mới  
- [ ] Đăng nhập tài khoản vô hiệu (`user_bi_khoa`)  
- [ ] Khách: chọn bàn → gọi món → theo dõi → hủy (PENDING + chưa duyệt)  
- [ ] Manager: duyệt đồ uống (đủ kho) / từ chối dòng  
- [ ] Chef: tiến trạng thái đến SERVED (đúng rule FOOD/DRINK)  
- [ ] Khách: thanh toán khi đủ SERVED → hóa đơn → bàn FREE  
- [ ] Manager: thanh toán hộ (order OPEN)  
- [ ] Khách: đánh giá order PAID (tổng thể + món)  
- [ ] Manager: xem review  
- [ ] Manager: CRUD món/bàn (một vài thao tác)  
- [ ] Manager: tạo chef / vô hiệu user (không phá rule 2 manager)  
- [ ] Manager: thống kê doanh thu + top món (khoảng có dữ liệu PAID)  

---

## 8. Lỗi thường gặp

- **Không kết nối được MySQL:** kiểm tra dịch vụ, `db.properties`, user/password.  
- **Không đủ dữ liệu thống kê:** import lại seed hoặc chọn đúng khoảng ngày có `checked_out_at` của order PAID.  
- **Không thanh toán được:** còn món chưa SERVED hoặc chưa duyệt QL (đồ uống / đồ ăn READY).  

---

*Tài liệu phản ánh menu tại thời điểm viết; nếu đổi số mục trong code, cập nhật bảng tương ứng.*
