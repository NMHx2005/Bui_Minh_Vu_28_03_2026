package com.restaurant.dao;

import com.restaurant.model.report.RevenueByDayRow;
import com.restaurant.model.report.TopDishRow;
import com.restaurant.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Báo cáo: orders PAID + checked_out_at; chi tiết món bỏ qua dòng CANCELLED (thống nhất thanh toán).
 */
public class ReportDAO {

    public BigDecimal sumRevenuePaidBetween(LocalDate fromInclusive, LocalDate toInclusive) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(o.total_amount), 0) AS s
                FROM orders o
                WHERE o.status = 'PAID'
                  AND o.checked_out_at IS NOT NULL
                  AND DATE(o.checked_out_at) BETWEEN ? AND ?
                """;
        try (var c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fromInclusive));
            ps.setDate(2, Date.valueOf(toInclusive));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("s");
                }
            }
        }
        return BigDecimal.ZERO;
    }

    public List<RevenueByDayRow> revenueByDayPaidBetween(LocalDate fromInclusive, LocalDate toInclusive)
            throws SQLException {
        String sql = """
                SELECT DATE(o.checked_out_at) AS d,
                       SUM(o.total_amount) AS revenue,
                       COUNT(*) AS cnt
                FROM orders o
                WHERE o.status = 'PAID'
                  AND o.checked_out_at IS NOT NULL
                  AND DATE(o.checked_out_at) BETWEEN ? AND ?
                GROUP BY DATE(o.checked_out_at)
                ORDER BY d ASC
                """;
        List<RevenueByDayRow> out = new ArrayList<>();
        try (var c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fromInclusive));
            ps.setDate(2, Date.valueOf(toInclusive));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new RevenueByDayRow(
                            rs.getDate("d").toLocalDate(),
                            rs.getBigDecimal("revenue"),
                            rs.getInt("cnt")));
                }
            }
        }
        return out;
    }

    public List<TopDishRow> topDishesByQuantity(LocalDate fromInclusive, LocalDate toInclusive, int limit)
            throws SQLException {
        return topDishes(fromInclusive, toInclusive, limit, "qty DESC, total_revenue DESC");
    }

    public List<TopDishRow> topDishesByRevenue(LocalDate fromInclusive, LocalDate toInclusive, int limit)
            throws SQLException {
        return topDishes(fromInclusive, toInclusive, limit, "total_revenue DESC, qty DESC");
    }

    private List<TopDishRow> topDishes(LocalDate fromInclusive, LocalDate toInclusive, int limit, String orderBy)
            throws SQLException {
        if (limit < 1) {
            limit = 10;
        }
        if (limit > 500) {
            limit = 500;
        }
        String sql = """
                SELECT mi.id AS mid, mi.name AS mname,
                       SUM(od.quantity) AS qty,
                       SUM(od.quantity * od.unit_price) AS total_revenue
                FROM order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                WHERE o.status = 'PAID'
                  AND o.checked_out_at IS NOT NULL
                  AND DATE(o.checked_out_at) BETWEEN ? AND ?
                  AND od.line_status <> 'CANCELLED'
                GROUP BY mi.id, mi.name
                ORDER BY """ + orderBy + " LIMIT ?";
        List<TopDishRow> out = new ArrayList<>();
        try (var c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fromInclusive));
            ps.setDate(2, Date.valueOf(toInclusive));
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new TopDishRow(
                            rs.getLong("mid"),
                            rs.getString("mname"),
                            rs.getLong("qty"),
                            rs.getBigDecimal("total_revenue")));
                }
            }
        }
        return out;
    }
}
