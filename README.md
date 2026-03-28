# Restaurant Management — Java Core + JDBC

## Chạy dự án

### 1. Yêu cầu

- **JDK 17+**
- **MySQL** (dịch vụ đang chạy)
- **Maven** không bắt buộc (repo có `./mvnw`)

### 2. Tạo database (một lần)

Từ thư mục gốc repo:

```bash
bash sql/init-db.sh
```

Lệnh này **xóa** database `restaurant_db` cũ (nếu có), rồi import toàn bộ `sql/schema.sql` (bảng + dữ liệu mẫu). Nhập **mật khẩu MySQL root** khi được hỏi.

Nếu **không** muốn xóa DB cũ, chỉ tạo khi chưa có:

```bash
mysql -u root -p < sql/schema.sql
```

### 3. Cấu hình kết nối

1. Sao chép `src/main/resources/db.properties.example` → `src/main/resources/db.properties`
2. Sửa `jdbc.url`, `jdbc.username`, `jdbc.password` cho đúng MySQL trên máy bạn.

Có thể ghi đè bằng biến môi trường: `JDBC_URL`, `JDBC_USER`, `JDBC_PASSWORD`.

### 4. Chạy ứng dụng (console)

```bash
./mvnw -q compile exec:java
```

Nếu đã cài Maven toàn máy:

```bash
mvn -q compile exec:java
```

### 5. Tài khoản mẫu (sau khi import `schema.sql`)

Mật khẩu đăng nhập ứng dụng: **`Manager@123`** (trừ tài khoản bị khóa).

| Username      | Vai trò   |
|---------------|-----------|
| manager       | Quản lý   |
| chef1, chef2  | Đầu bếp   |
| customer1, customer2, customer_demo | Khách |
| user_bi_khoa  | Khách (vô hiệu — không đăng nhập được) |

(Mật khẩu ứng dụng khác mật khẩu **root** MySQL.)
