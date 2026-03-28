# Restaurant Management — Java Core + JDBC

## Buổi 2 (đã triển khai)

- Đăng nhập (sai thì nhập lại; nhập `0` làm tên đăng nhập để quay lại menu chính).
- Đăng ký khách hàng (validate, username không trùng, lưu bcrypt).
- Menu **Quản lý**: CRUD + tìm kiếm thực đơn (xóa = ẩn `is_active`), CRUD + tìm bàn theo mã.
- **Đầu bếp / Khách**: placeholder (buổi 3–4).

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

Chương trình in số user và kiểm tra bcrypt cho tài khoản seed `manager`.

### Tài khoản mẫu (sau khi chạy `schema.sql`)

| Username | Mật khẩu    | Vai trò |
|----------|-------------|---------|
| manager  | Manager@123 | MANAGER |
| chef1    | Manager@123 | CHEF    |

(Mật khẩu ứng dụng; khác với mật khẩu **root** MySQL.)

### Ghi chú schema

- Bàn ăn nằm trong bảng `dining_tables` (tránh nhầm với từ khóa SQL `TABLES`).
- Cột `users.is_active` phục vụ tính năng nâng cao (vô hiệu hóa tài khoản).

### Cấu trúc package

`model` → `dao` → `service` → `presentation`, cùng `util` (`DBConnection`, `PasswordHasher`).
