package com.restaurant.dao;

import com.restaurant.model.OrderStatus;
import com.restaurant.model.RestaurantOrder;
import com.restaurant.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class OrderDAO {

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
}
