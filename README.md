# Restaurant Management — Java Core + JDBC

## Buổi 4 (đã triển khai)

- Đầu bếp: xem hàng đợi (order **OPEN**, `manager_approval = APPROVED`, trạng thái PENDING / COOKING / READY), sắp xếp theo `created_at`.
- Cập nhật **đúng một bước** mỗi lần: PENDING→COOKING→READY→SERVED.
- Khách xem lại món từ DB (không cache).

## Buổi 3 (đã triển khai)

- Khách: xem thực đơn (chỉ `is_active`), nhóm đồ ăn / đồ uống.
- Chọn **bàn trống** → transaction: `dining_tables` → `OCCUPIED` + tạo `orders` trạng thái `OPEN`.
- **Gọi món:** snapshot `unit_price`, dòng `PENDING`; đồ uống trừ tồn kho trong cùng transaction.
- **Theo dõi / hủy:** chỉ hủy khi `PENDING`, đánh dấu `CANCELLED`, hoàn kho đồ uống, cập nhật `total_amount`.
- Mọi truy vấn chi tiết order đều ràng `customer_user_id` của khách đăng nhập.

## Buổi 2 (đã triển khai)

- Đăng nhập (sai thì nhập lại; nhập `0` làm tên đăng nhập để quay lại menu chính).
- Đăng ký khách hàng (validate, username không trùng, lưu bcrypt).
- Menu **Quản lý**: CRUD + tìm kiếm thực đơn (xóa = ẩn `is_active`), CRUD + tìm bàn theo mã.
- **Đầu bếp** (buổi 4): hàng đợi bếp, chuyển trạng thái tuần tự PENDING→COOKING→READY→SERVED (chỉ dòng `manager_approval = APPROVED`).

Chạy ứng dụng:

```bash
./mvnw -q compile exec:java
```

## Buổi 1

### Yêu cầu

- JDK 17+
- Maven 3.9+
- MySQL Server (đã chạy dịch vụ)

### Tạo cơ sở dữ liệu

```bash
mysql -u root -p < sql/schema.sql
```

Nhập mật khẩu MySQL của bạn khi được hỏi.

### Cấu hình kết nối

1. Sao chép `src/main/resources/db.properties.example` thành `db.properties` (nếu chưa có).
2. Chỉnh `jdbc.url`, `jdbc.username`, `jdbc.password` cho đúng máy bạn.

File `db.properties` đã được liệt kê trong `.gitignore` để tránh đẩy mật khẩu lên git.

Có thể ghi đè bằng biến môi trường: `JDBC_URL`, `JDBC_USER`, `JDBC_PASSWORD`.

### Chạy kiểm tra kết nối

**Cách 1 — không cần cài Maven:** dùng wrapper có sẵn trong repo (lần đầu sẽ tải Maven về thư mục `~/.m2/wrapper`):

```bash
./mvnw -q compile exec:java
```

**Cách 2 — đã cài Maven toàn máy:**

```bash
mvn -q compile exec:java
```

Nếu gặp `command not found: mvn` thì dùng `./mvnw` hoặc cài Maven (macOS Homebrew: `brew install maven`).

### Tài khoản mẫu (sau khi chạy `sql/schema.sql`)

Cùng mật khẩu ứng dụng **`Manager@123`** (bcrypt trong SQL), trừ `user_bi_khoa` (`is_active = 0` — đăng nhập bị từ chối).

| Username      | Vai trò   | Ghi chú |
|---------------|-----------|---------|
| manager       | MANAGER   | CRUD, thống kê, duyệt món (nâng cao) |
| manager_phu   | MANAGER   | Manager thứ 2 — test **không vô hiệu hóa manager cuối cùng** (prompt 07) |
| chef1, chef2  | CHEF      | Bếp (buổi 4) |
| customer1     | CUSTOMER  | Không có order OPEN — **chọn bàn + gọi món** (buổi 3); có lịch sử PAID + review |
| customer2     | CUSTOMER  | Order **OPEN** #4, bàn **T08** |
| customer_demo | CUSTOMER  | Order **OPEN** #5, bàn **T09** — nhiều trạng thái bếp + 1 dòng chờ duyệt manager |
| user_bi_khoa  | CUSTOMER  | Vô hiệu — test login |

### Dữ liệu demo đầy đủ (theo `prompts/` & buổi 2–5 + nâng cao)

File **`sql/schema.sql`** gồm DDL + seed phục vụ cả code hiện tại và các prompt trong thư mục **`prompts/`** (trước/sau khi implement).

| Prompt / nội dung | Dữ liệu seed |
|-------------------|--------------|
| **01–03** | User, bàn, menu, order OPEN #4 (customer2), #5 (customer_demo), tồn kho đồ uống đã chỉnh theo giao dịch |
| **04** (Chef) | Nhiều dòng `PENDING` / `COOKING` / `READY` / `SERVED` trên order #4 và #5; sắp xếp theo `created_at` |
| **05** | PAID / OPEN / CANCELLED / dòng `CANCELLED` + `REJECTED`; bàn `OCCUPIED` vs `FREE` |
| **06** Thanh toán | Orders **#1–#3** `PAID` + `checked_out_at` (2026-03-14, 20, 26); `total_amount` khớp chi tiết |
| **07** Quản lý user | `user_bi_khoa` inactive; **hai** manager active |
| **08** Duyệt món | Cột `order_details.manager_approval` (`APPROVED` / `PENDING` / `REJECTED`); order #5 có 1 dòng Coca `manager_approval = PENDING` |
| **09** Review | Bảng **`reviews`** + 3 bản ghi (theo order PAID) |
| **10** Thống kê | Nhiều order PAID + chi tiết `SERVED` để GROUP BY doanh thu / top món |

**Bàn:** `T01`–`T07`, `VIP01`, `BAR02`, `T10` = **FREE** (bàn đã dùng cho order PAID đã “trả”). **`T08`, `T09` = OCCUPIED** (hai order OPEN).

**Order tóm tắt:** `#1–#3` PAID; `#4` OPEN customer2 @ T08; `#5` OPEN customer_demo @ T09; `#6` CANCELLED.

**Lưu ý kỹ thuật:** code Java hiện `INSERT order_details` **không** gửi `manager_approval` — MySQL dùng mặc định **`APPROVED`** nên không cần sửa DAO ngay khi import schema mới.

**Import lại:** `DROP DATABASE IF EXISTS restaurant_db;` rồi chạy lại `schema.sql` (tránh trùng username / mã bàn).

(Mật khẩu ứng dụng khác mật khẩu **root** MySQL.)

### Ghi chú schema

- Bàn: `dining_tables`.
- `orders.checked_out_at` — dùng cho thanh toán & báo cáo theo ngày.
- `order_details.manager_approval` — nâng cao duyệt món (mặc định `APPROVED`).
- Bảng `reviews` — nâng cao đánh giá.

### Cấu trúc package

`model` → `dao` → `service` → `presentation`, cùng `util` (`DBConnection`, `PasswordHasher`).
