package com.restaurant.util;

import com.restaurant.model.DiningTable;
import com.restaurant.model.MenuItem;
import com.restaurant.model.MenuItemType;
import com.restaurant.model.TableStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

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
        return String.format("%,.0f VNĐ", v);
    }
}
