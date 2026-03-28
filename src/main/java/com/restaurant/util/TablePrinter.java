package com.restaurant.util;

import com.restaurant.model.CheckoutInvoice;
import com.restaurant.model.ChefKitchenLine;
import com.restaurant.model.DiningTable;
import com.restaurant.model.MenuItem;
import com.restaurant.model.MenuItemType;
import com.restaurant.model.OrderLineStatus;
import com.restaurant.model.OrderLineView;
import com.restaurant.model.TableStatus;

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
        String line = "--------------------------------------------------------------------------------------";
        System.out.println(line);
        System.out.printf(Locale.forLanguageTag("vi-VN"),
                "%-8s %-28s %6s %12s %14s %12s%n",
                "ID dòng", "Món", "SL", "Đơn giá", "Thành tiền", "Trạng thái");
        System.out.println(line);
        for (OrderLineView v : lines) {
            System.out.printf(Locale.forLanguageTag("vi-VN"),
                    "%-8d %-28s %6d %,12.0f %,14.0f %12s%n",
                    v.getDetailId(),
                    truncate(v.getMenuItemName(), 28),
                    v.getQuantity(),
                    v.getUnitPrice(),
                    v.getLineTotal(),
                    orderLineStatusVi(v.getLineStatus()));
        }
        System.out.println(line);
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
        String sep = "--------------------------------------------------------------------------------------------------------";
        System.out.println(sep);
        System.out.printf("%-8s %-8s %-10s %-26s %5s %-12s %14s%n",
                "ID dòng", "Order", "Bàn", "Món", "SL", "Trạng thái", "Giờ tạo");
        System.out.println(sep);
        for (ChefKitchenLine r : lines) {
            System.out.printf("%-8d %-8d %-10s %-26s %5d %-12s %14s%n",
                    r.getDetailId(),
                    r.getOrderId(),
                    truncate(r.getTableCode(), 10),
                    truncate(r.getMenuItemName(), 26),
                    r.getQuantity(),
                    orderLineStatusVi(r.getLineStatus()),
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
}
