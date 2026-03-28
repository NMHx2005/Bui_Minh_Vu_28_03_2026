package com.restaurant.util;

import com.restaurant.dao.OrderDAO;
import com.restaurant.dao.ReviewDAO;
import com.restaurant.model.CheckoutInvoice;
import com.restaurant.model.Role;
import com.restaurant.model.ChefKitchenLine;
import com.restaurant.model.DiningTable;
import com.restaurant.model.MenuItem;
import com.restaurant.model.ManagerApproval;
import com.restaurant.model.MenuItemType;
import com.restaurant.model.OrderLineStatus;
import com.restaurant.model.OrderLineView;
import com.restaurant.model.ReviewListRow;
import com.restaurant.model.TableStatus;
import com.restaurant.model.User;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class TablePrinter {

    private TablePrinter() {
    }

    public static void printMenuItemsTable(List<MenuItem> items) {
        if (items.isEmpty()) {
            System.out.println("(Không có món nào.)");
            return;
        }
        String line = "------------------------------------------------------------------------------------------";
        System.out.println(line);
        System.out.printf("%-6s %-28s %-8s %14s %10s %8s%n",
                "ID", "Tên món", "Loại", "Giá (VNĐ)", "Tồn kho", "Hiệu lực");
        System.out.println(line);
        for (MenuItem m : items) {
            String stock = m.getItemType() == MenuItemType.DRINK && m.getStockQuantity() != null
                    ? String.valueOf(m.getStockQuantity())
                    : "-";
            String active = m.isActive() ? "Có" : "Không";
            System.out.printf(Locale.forLanguageTag("vi-VN"),
                    "%-6d %-28s %-8s %,14.0f %10s %8s%n",
                    m.getId(),
                    truncate(m.getName(), 28),
                    m.getItemType() == MenuItemType.FOOD ? "Đồ ăn" : "Đồ uống",
                    m.getPrice(),
                    stock,
                    active);
        }
        System.out.println(line);
    }

    public static void printUsersAdminTable(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("(Chưa có người dùng nào.)");
            return;
        }
        String line = "------------------------------------------------------------------------";
        System.out.println(line);
        System.out.printf("%-8s %-26s %-12s %12s%n", "ID", "Username", "Vai trò", "Hoạt động");
        System.out.println(line);
        for (User u : users) {
            System.out.printf("%-8d %-26s %-12s %12s%n",
                    u.getId() != null ? u.getId() : 0L,
                    truncate(u.getUsername(), 26),
                    roleVi(u.getRole()),
                    u.isActive() ? "Có" : "Không");
        }
        System.out.println(line);
    }

    private static String roleVi(Role r) {
        return switch (r) {
            case MANAGER -> "Quản lý";
            case CHEF -> "Đầu bếp";
            case CUSTOMER -> "Khách";
        };
    }

    public static void printDiningTablesTable(List<DiningTable> tables) {
        if (tables.isEmpty()) {
            System.out.println("(Không có bàn nào.)");
            return;
        }
        String line = "-------------------------------------------------------------------";
        System.out.println(line);
        System.out.printf("%-6s %-14s %10s %12s%n", "ID", "Mã bàn", "Sức chứa", "Trạng thái");
        System.out.println(line);
        for (DiningTable t : tables) {
            String st = t.getStatus() == TableStatus.FREE ? "Trống" : "Đang dùng";
            System.out.printf("%-6d %-14s %10d %12s%n",
                    t.getId(),
                    truncate(t.getTableCode(), 14),
                    t.getCapacity(),
                    st);
        }
        System.out.println(line);
    }

    private static String truncate(String s, int maxChars) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars - 1) + "…";
    }

    /** In một dòng giá (dùng khi sửa món). */
    public static String formatMoney(BigDecimal v) {
        return String.format(Locale.forLanguageTag("vi-VN"), "%,.0f VNĐ", v);
    }

    /** Thực đơn cho khách: nhóm Đồ ăn / Đồ uống. */
    public static void printCustomerMenu(List<MenuItem> items) {
        if (items.isEmpty()) {
            System.out.println("(Hiện không có món đang phục vụ.)");
            return;
        }
        List<MenuItem> foods = items.stream().filter(m -> m.getItemType() == MenuItemType.FOOD).collect(Collectors.toCollection(ArrayList::new));
        List<MenuItem> drinks = items.stream().filter(m -> m.getItemType() == MenuItemType.DRINK).collect(Collectors.toCollection(ArrayList::new));
        if (!foods.isEmpty()) {
            System.out.println("--- Đồ ăn ---");
            printCustomerMenuSection(foods);
        }
        if (!drinks.isEmpty()) {
            System.out.println("--- Đồ uống ---");
            printCustomerMenuSection(drinks);
        }
    }

    private static void printCustomerMenuSection(List<MenuItem> section) {
        String line = "------------------------------------------------------------------------";
        System.out.println(line);
        System.out.printf(Locale.forLanguageTag("vi-VN"), "%-6s %-32s %14s %10s%n", "ID", "Tên", "Giá (VNĐ)", "Tồn");
        System.out.println(line);
        for (MenuItem m : section) {
            String stock = m.getStockQuantity() != null ? String.valueOf(m.getStockQuantity()) : "-";
            System.out.printf(Locale.forLanguageTag("vi-VN"), "%-6d %-32s %,14.0f %10s%n",
                    m.getId(), truncate(m.getName(), 32), m.getPrice(), stock);
        }
        System.out.println(line);
    }

    public static void printOrderLinesTable(List<OrderLineView> lines) {
        if (lines.isEmpty()) {
            System.out.println("(Chưa có món nào trong order.)");
            return;
        }
        String line = "--------------------------------------------------------------------------------------------------------------";
        System.out.println(line);
        System.out.printf(Locale.forLanguageTag("vi-VN"),
                "%-8s %-24s %4s %6s %12s %14s %10s %10s%n",
                "ID dòng", "Món", "Loại", "SL", "Đơn giá", "Thành tiền", "Bếp", "QL duyệt");
        System.out.println(line);
        for (OrderLineView v : lines) {
            System.out.printf(Locale.forLanguageTag("vi-VN"),
                    "%-8d %-24s %4s %6d %,12.0f %,14.0f %10s %10s%n",
                    v.getDetailId(),
                    truncate(v.getMenuItemName(), 24),
                    v.getItemType() == MenuItemType.FOOD ? "Ăn" : "Uống",
                    v.getQuantity(),
                    v.getUnitPrice(),
                    v.getLineTotal(),
                    orderLineStatusVi(v.getLineStatus()),
                    managerApprovalVi(v.getManagerApproval()));
        }
        System.out.println(line);
    }

    public static String managerApprovalVi(ManagerApproval a) {
        if (a == null) {
            return "-";
        }
        return switch (a) {
            case PENDING -> "Chờ";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Từ chối";
        };
    }

    /** Tổng thành tiền các dòng không CANCELLED (dùng trước khi xác nhận thanh toán). */
    public static BigDecimal sumBillableSubtotal(List<OrderLineView> lines) {
        BigDecimal s = BigDecimal.ZERO;
        for (OrderLineView v : lines) {
            if (v.getLineStatus() == OrderLineStatus.CANCELLED) {
                continue;
            }
            s = s.add(v.getLineTotal());
        }
        return s;
    }

    private static final DateTimeFormatter INVOICE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Hóa đơn sau khi checkout (bảng + printf). */
    public static void printCheckoutInvoice(CheckoutInvoice inv) {
        String sep = "==========================================================================================";
        System.out.println(sep);
        System.out.println("                         HÓA ĐƠN THANH TOÁN");
        System.out.println(sep);
        System.out.printf("Order: #%d    Bàn: %s%n", inv.getOrderId(), inv.getTableCode() != null ? inv.getTableCode() : "-");
        System.out.printf("Thời điểm thanh toán: %s%n", inv.getCheckedOutAt().format(INVOICE_TIME));
        System.out.println(sep);
        System.out.printf(Locale.forLanguageTag("vi-VN"),
                "%-4s %-32s %6s %14s %16s%n", "STT", "Món", "SL", "Đơn giá", "Thành tiền");
        System.out.println(sep);
        List<OrderLineView> bill = inv.getBillableLines();
        if (bill == null || bill.isEmpty()) {
            System.out.println("(Không có dòng tính phí.)");
        } else {
            int i = 1;
            for (OrderLineView v : bill) {
                System.out.printf(Locale.forLanguageTag("vi-VN"),
                        "%-4d %-32s %6d %,14.0f %,16.0f%n",
                        i++,
                        truncate(v.getMenuItemName(), 32),
                        v.getQuantity(),
                        v.getUnitPrice(),
                        v.getLineTotal());
            }
        }
        System.out.println(sep);
        System.out.printf(Locale.forLanguageTag("vi-VN"),
                "%58s %,16.0f VNĐ%n", "TỔNG CỘNG:", inv.getTotalAmount());
        System.out.println(sep);
        System.out.println("Cảm ơn quý khách.");
    }

    private static final DateTimeFormatter CHEF_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    public static void printChefKitchenQueue(List<ChefKitchenLine> lines) {
        if (lines.isEmpty()) {
            System.out.println("(Không có món nào đang chờ bếp xử lý.)");
            return;
        }
        String sep = "------------------------------------------------------------------------------------------------------------------------";
        System.out.println(sep);
        System.out.printf("%-8s %-8s %-10s %-22s %4s %5s %-10s %-10s %12s%n",
                "ID dòng", "Order", "Bàn", "Món", "Loại", "SL", "Bếp", "QL", "Giờ tạo");
        System.out.println(sep);
        for (ChefKitchenLine r : lines) {
            String loai = r.getItemType() == MenuItemType.FOOD ? "Ăn" : "Uống";
            System.out.printf("%-8d %-8d %-10s %-22s %4s %5d %-10s %-10s %12s%n",
                    r.getDetailId(),
                    r.getOrderId(),
                    truncate(r.getTableCode(), 10),
                    truncate(r.getMenuItemName(), 22),
                    loai,
                    r.getQuantity(),
                    orderLineStatusVi(r.getLineStatus()),
                    r.getManagerApproval() != null ? managerApprovalVi(r.getManagerApproval()) : "-",
                    r.getCreatedAt().format(CHEF_TIME));
        }
        System.out.println(sep);
    }

    public static String orderLineStatusVi(OrderLineStatus s) {
        return switch (s) {
            case PENDING -> "Chờ";
            case COOKING -> "Đang nấu";
            case READY -> "Sẵn sàng";
            case SERVED -> "Đã phục vụ";
            case CANCELLED -> "Đã hủy";
        };
    }

    private static final DateTimeFormatter REVIEW_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void printPaidOrdersSummary(List<OrderDAO.PaidOrderSummary> orders) {
        if (orders.isEmpty()) {
            System.out.println("(Chưa có order đã thanh toán để đánh giá.)");
            return;
        }
        String line = "----------------------------------------------------------------------------";
        System.out.println(line);
        System.out.printf(Locale.forLanguageTag("vi-VN"),
                "%-10s %-12s %16s %20s%n", "Order #", "Bàn", "Tổng (VNĐ)", "Thanh toán");
        System.out.println(line);
        for (OrderDAO.PaidOrderSummary o : orders) {
            String when = o.checkedOutAt() != null ? o.checkedOutAt().format(REVIEW_TIME) : "-";
            System.out.printf(Locale.forLanguageTag("vi-VN"),
                    "%-10d %-12s %,16.0f %20s%n",
                    o.orderId(), truncate(o.tableCode(), 12), o.totalAmount(), when);
        }
        System.out.println(line);
    }

    public static void printDishReviewOptions(List<ReviewDAO.DishOption> options) {
        if (options.isEmpty()) {
            System.out.println("(Không có món hợp lệ trong order này.)");
            return;
        }
        String line = "----------------------------------------";
        System.out.println(line);
        System.out.printf("%-10s %s%n", "ID món", "Tên");
        System.out.println(line);
        for (ReviewDAO.DishOption d : options) {
            System.out.printf("%-10d %s%n", d.menuItemId(), d.menuItemName());
        }
        System.out.println(line);
    }

    public static void printReviewsTable(List<ReviewListRow> rows) {
        if (rows.isEmpty()) {
            System.out.println("(Chưa có đánh giá nào.)");
            return;
        }
        String sep = "------------------------------------------------------------------------------------------------------------------------";
        System.out.println(sep);
        System.out.printf("%-6s %-14s %-8s %-22s %4s %-30s %16s%n",
                "ID", "Khách", "Order", "Món", "Sao", "Bình luận", "Thời gian");
        System.out.println(sep);
        for (ReviewListRow r : rows) {
            String dish = r.getMenuItemName() != null ? truncate(r.getMenuItemName(), 22) : "(Cả order)";
            String oid = r.getOrderId() != null ? String.valueOf(r.getOrderId()) : "-";
            String cmt = r.getComment() != null ? truncate(r.getComment(), 30) : "-";
            System.out.printf("%-6d %-14s %-8s %-22s %4d %-30s %16s%n",
                    r.getId(),
                    truncate(r.getUsername(), 14),
                    oid,
                    dish,
                    r.getRating(),
                    cmt,
                    r.getCreatedAt().format(REVIEW_TIME));
        }
        System.out.println(sep);
    }
}
