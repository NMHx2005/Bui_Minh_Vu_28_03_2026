# Restaurant Management — Java Core + JDBC

## Nâng cao — Thanh toán & hóa đơn (06)

- **Khách (menu 6):** chỉ thanh toán **order OPEN** của chính tài khoản đang đăng nhập (`findOpenOrderForCustomer`). Không thể thanh toán order của người khác.
- **Quản lý (menu 3):** nhập **mã order OPEN** bất kỳ → xem chi tiết dòng → xác nhận → thanh toán.
- **Điều kiện:** trong một transaction: `orders` → **PAID**, `checked_out_at` = NOW, `total_amount` = tổng `quantity * unit_price` (bỏ qua dòng **CANCELLED**), bàn → **FREE** (và xóa `current_order_id` nếu schema có).
- **Quy ước bếp:** chỉ cho checkout khi **mọi dòng không CANCELLED** đều **SERVED** (đồng bộ với luồng Chef buổi 4).
- Sau thanh toán: in **hóa đơn** dạng bảng trên console (`TablePrinter.printCheckoutInvoice`).

## Nâng cao — Quản lý người dùng (07)

- **`users.is_active`:** đã có trong `sql/schema.sql`; DB cũ có thể chạy `sql/migration_07_users_is_active.sql` (bỏ qua nếu cột đã tồn tại).
- **Đăng nhập:** `is_active = 0` → từ chối, thông báo *Tài khoản đã bị vô hiệu hóa* (sau khi xác thực mật khẩu đúng).
- **Quản lý — menu 4:** liệt kê user (id, username, role, hoạt động); tạo **CHEF** (username/mật khẩu, bcrypt, kiểm tra trùng tên); **vô hiệu hóa** theo id + xác nhận.
- **An toàn:** không tự vô hiệu chính mình; không vô hiệu **manager cuối cùng** đang active (`UserAdminService` + `UserDAO.countActiveByRole`).
- **Tầng code:** `UserAdminService`, `UserAdminPresentation`, mở rộng `UserDAO` / `AuthService` (validate tái dùng cho tạo đầu bếp). Đăng ký khách không đổi.

## Nâng cao — Duyệt món (08)

**Mô hình nghiệp vụ (một luồng nhất quán):**

- **Đồ ăn (FOOD):** Khách gọi → `manager_approval = PENDING`, bếp nấu PENDING→COOKING→READY **trước** khi quản lý duyệt. Quản lý **chỉ duyệt khi dòng đã READY** (“đã nấu xong”). Sau khi duyệt (`APPROVED`), bếp mới chuyển READY→SERVED.
- **Đồ uống (DRINK):** Khách gọi chỉ **kiểm tra** tồn kho đủ; **trừ kho trong transaction** khi quản lý duyệt. Bếp chỉ thấy/xử lý đồ uống sau `APPROVED`.
- **Từ chối:** `REJECTED` + `line_status = CANCELLED`, cập nhật `total_amount`. Đồ uống chưa duyệt nên **không hoàn kho** khi khách hủy (chưa trừ).

**Quản lý — menu 5:** xem order OPEN, duyệt/từ chối từng dòng hoặc duyệt hàng loạt (`ManagerDishApprovalService` + `ManagerDishApprovalPresentation`). Migration tùy chọn: `sql/migration_08_manager_approval_default.sql`.

## Buổi 5 (đã rà soát)

- Nhập liệu: số / chuỗi rỗng / khoảng trắng xử lý rõ ràng tiếng Việt (`ConsoleIO`, `readNonBlankLine` cho tên/mã/từ khóa).
- Chọn bàn: thông báo cụ thể khi bàn **OCCUPIED** hoặc không hợp lệ; gợi ý chỉ chọn ID trong danh sách FREE.
- Lỗi không bắt được ở tầng con: `RestaurantApp` bắt `Exception`, **không in stack trace** ra người dùng.
- Menu phụ: ghi chú **0 = Quay lại / Đăng xuất** trên tiêu đề một số màn hình.
- Checklist kiểm thử: **`TESTING.md`**.

**Ghi chú:** order **OPEN** giữ bàn **OCCUPIED** cho đến khi **thanh toán** (khách mục 6 hoặc quản lý mục 3); sau đó bàn **FREE**. Quản lý người dùng: **mục 4** (**07**). Duyệt món: **mục 5** (**08**).

## Buổi 4 (đã triển khai)

- Đầu bếp: hàng đợi theo nghiệp vụ **08** — đồ ăn (chưa / đã duyệt tùy bước), đồ uống chỉ sau `APPROVED`; sắp xếp theo `created_at`.
- Cập nhật **một bước** PENDING→COOKING→READY→SERVED; đồ ăn READY **chưa duyệt** thì không chuyển được sang SERVED.
- Khách xem lại món từ DB (không cache).

## Buổi 3 (đã triển khai)

- Khách: xem thực đơn (chỉ `is_active`), nhóm đồ ăn / đồ uống.
- Chọn **bàn trống** → transaction: `dining_tables` → `OCCUPIED` + tạo `orders` trạng thái `OPEN`.
- **Gọi món:** snapshot `unit_price`, dòng `PENDING`, `manager_approval = PENDING`; đồ uống **chỉ kiểm tra** tồn kho (trừ kho khi quản lý duyệt — **08**).
- **Theo dõi / hủy:** chỉ khi `PENDING` bếp và `manager_approval = PENDING`; `CANCELLED`, cập nhật `total_amount` (không hoàn kho đồ uống vì chưa trừ).
- Mọi truy vấn chi tiết order đều ràng `customer_user_id` của khách đăng nhập.

## Buổi 2 (đã triển khai)

- Đăng nhập (sai thì nhập lại; nhập `0` làm tên đăng nhập để quay lại menu chính).
- Đăng ký khách hàng (validate, username không trùng, lưu bcrypt).
- Menu **Quản lý**: CRUD + tìm kiếm thực đơn (xóa = ẩn `is_active`), CRUD + tìm bàn theo mã; **nâng cao:** thanh toán, user, duyệt món.
- **Đầu bếp** (buổi 4 + **08**): hàng đợi và bước chuyển trạng thái theo `OrderDetailDAO` / `ChefService`.

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
| **01–03** | User, bàn, menu, order OPEN #4 (customer2), #5 (customer_demo); tồn kho seed — trừ kho thật khi duyệt đồ uống (**08**) |
| **04** (Chef) | Order #4/#5: đủ bước bếp; kết hợp **08** (đồ uống chờ duyệt trên #5) |
| **05** | PAID / OPEN / CANCELLED / dòng `CANCELLED` + `REJECTED`; bàn `OCCUPIED` vs `FREE` |
| **06** Thanh toán | Orders **#1–#3** `PAID` + `checked_out_at` (2026-03-14, 20, 26); `total_amount` khớp chi tiết |
| **07** Quản lý user | `user_bi_khoa` inactive; **hai** manager active |
| **08** Duyệt món | Cột `order_details.manager_approval` (`APPROVED` / `PENDING` / `REJECTED`); order #5 có 1 dòng Coca `manager_approval = PENDING` |
| **09** Review | Bảng **`reviews`** + 3 bản ghi (theo order PAID) |
| **10** Thống kê | Nhiều order PAID + chi tiết `SERVED` để GROUP BY doanh thu / top món |

**Bàn:** `T01`–`T07`, `VIP01`, `BAR02`, `T10` = **FREE** (bàn đã dùng cho order PAID đã “trả”). **`T08`, `T09` = OCCUPIED** (hai order OPEN).

**Order tóm tắt:** `#1–#3` PAID; `#4` OPEN customer2 @ T08; `#5` OPEN customer_demo @ T09; `#6` CANCELLED.

**Lưu ý kỹ thuật:** `INSERT order_details` ghi rõ `manager_approval = PENDING` (đồng bộ DDL default **PENDING**).

**Import lại:** `DROP DATABASE IF EXISTS restaurant_db;` rồi chạy lại `schema.sql` (tránh trùng username / mã bàn).

(Mật khẩu ứng dụng khác mật khẩu **root** MySQL.)

### Ghi chú schema

- Bàn: `dining_tables`.
- `orders.checked_out_at` — dùng cho thanh toán & báo cáo theo ngày.
- `order_details.manager_approval` — duyệt món (**08**); mặc định `PENDING` cho dòng mới.
- Bảng `reviews` — nâng cao đánh giá.

### Cấu trúc package

`model` → `dao` → `service` → `presentation`, cùng `util` (`DBConnection`, `PasswordHasher`).
