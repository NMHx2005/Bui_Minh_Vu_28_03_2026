package com.restaurant.dao;

import com.restaurant.model.OrderStatus;
import com.restaurant.model.RestaurantOrder;
import com.restaurant.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDAO {

    public record OpenOrderSummary(long orderId, long tableId, String tableCode) {
    }

    public record PaidOrderSummary(long orderId, String tableCode, BigDecimal totalAmount,
                                   LocalDateTime checkedOutAt) {
    }

    private static RestaurantOrder mapOrder(ResultSet rs) throws SQLException {
        RestaurantOrder o = new RestaurantOrder();
        o.setId(rs.getLong("id"));
        o.setTableId(rs.getLong("table_id"));
        long cid = rs.getLong("customer_user_id");
        if (rs.wasNull()) {
            o.setCustomerUserId(null);
        } else {
            o.setCustomerUserId(cid);
        }
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        var co = rs.getTimestamp("checked_out_at");
        o.setCheckedOutAt(co != null ? co.toLocalDateTime() : null);
        o.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        o.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return o;
    }

    public Optional<RestaurantOrder> findOpenOrderForCustomer(long customerUserId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return findOpenOrderForCustomer(c, customerUserId);
        }
    }

    public Optional<RestaurantOrder> findOpenOrderForCustomer(Connection c, long customerUserId) throws SQLException {
        String sql = """
                SELECT id, table_id, customer_user_id, status, total_amount, checked_out_at, created_at, updated_at
                FROM orders
                WHERE customer_user_id = ? AND status = 'OPEN'
                LIMIT 1
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, customerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapOrder(rs));
                }
            }
        }
        return Optional.empty();
    }

    public long insertOpenOrder(Connection c, long tableId, long customerUserId) throws SQLException {
        String sql = "INSERT INTO orders (table_id, customer_user_id, status, total_amount) VALUES (?, ?, 'OPEN', 0.00)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, tableId);
            ps.setLong(2, customerUserId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Không lấy được id order mới.");
    }

    public void recalculateTotalAmount(Connection c, long orderId) throws SQLException {
        String sql = """
                UPDATE orders o
                SET total_amount = COALESCE((
                    SELECT SUM(od.quantity * od.unit_price)
                    FROM order_details od
                    WHERE od.order_id = o.id AND od.line_status <> 'CANCELLED'
                ), 0)
                WHERE o.id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.executeUpdate();
        }
    }

    public Optional<RestaurantOrder> findById(long orderId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return findById(c, orderId);
        }
    }

    public Optional<RestaurantOrder> findById(Connection c, long orderId) throws SQLException {
        String sql = """
                SELECT id, table_id, customer_user_id, status, total_amount, checked_out_at, created_at, updated_at
                FROM orders WHERE id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapOrder(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<RestaurantOrder> findOpenOrderById(long orderId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return findOpenById(c, orderId);
        }
    }

    public Optional<RestaurantOrder> findOpenById(Connection c, long orderId) throws SQLException {
        String sql = """
                SELECT id, table_id, customer_user_id, status, total_amount, checked_out_at, created_at, updated_at
                FROM orders WHERE id = ? AND status = 'OPEN'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapOrder(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Đánh dấu PAID, ghi checkout, cập nhật total từ chi tiết (không tính CANCELLED). Chỉ khi đang OPEN.
     *
     * @return số dòng cập nhật (1 nếu thành công)
     */
    public int markOrderPaidIfOpen(Connection c, long orderId) throws SQLException {
        String sql = """
                UPDATE orders o
                SET o.status = 'PAID',
                    o.checked_out_at = NOW(),
                    o.total_amount = COALESCE((
                        SELECT SUM(od.quantity * od.unit_price)
                        FROM order_details od
                        WHERE od.order_id = o.id AND od.line_status <> 'CANCELLED'
                    ), 0)
                WHERE o.id = ? AND o.status = 'OPEN'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            return ps.executeUpdate();
        }
    }

    public List<OpenOrderSummary> listOpenOrderSummaries() throws SQLException {
        String sql = """
                SELECT o.id, o.table_id, dt.table_code
                FROM orders o
                INNER JOIN dining_tables dt ON dt.id = o.table_id
                WHERE o.status = 'OPEN'
                ORDER BY o.id ASC
                """;
        List<OpenOrderSummary> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new OpenOrderSummary(
                        rs.getLong("id"),
                        rs.getLong("table_id"),
                        rs.getString("table_code")));
            }
        }
        return out;
    }

    public List<PaidOrderSummary> listPaidOrdersForCustomer(long customerUserId) throws SQLException {
        String sql = """
                SELECT o.id, dt.table_code, o.total_amount, o.checked_out_at
                FROM orders o
                INNER JOIN dining_tables dt ON dt.id = o.table_id
                WHERE o.customer_user_id = ? AND o.status = 'PAID'
                ORDER BY o.checked_out_at DESC, o.id DESC
                """;
        List<PaidOrderSummary> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, customerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    var co = rs.getTimestamp("checked_out_at");
                    out.add(new PaidOrderSummary(
                            rs.getLong("id"),
                            rs.getString("table_code"),
                            rs.getBigDecimal("total_amount"),
                            co != null ? co.toLocalDateTime() : null));
                }
            }
        }
        return out;
    }
}
