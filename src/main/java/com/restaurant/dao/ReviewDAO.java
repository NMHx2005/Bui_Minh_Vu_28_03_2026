package com.restaurant.dao;

import com.restaurant.model.ReviewListRow;
import com.restaurant.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReviewDAO {

    public record DishOption(long menuItemId, String menuItemName) {
    }

    public boolean existsOrderLevelReview(Connection c, long userId, long orderId) throws SQLException {
        String sql = """
                SELECT 1 FROM reviews
                WHERE user_id = ? AND order_id = ? AND menu_item_id IS NULL
                LIMIT 1
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsDishReview(Connection c, long userId, long orderId, long menuItemId) throws SQLException {
        String sql = """
                SELECT 1 FROM reviews
                WHERE user_id = ? AND order_id = ? AND menu_item_id = ?
                LIMIT 1
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, orderId);
            ps.setLong(3, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insert(Connection c, long userId, long orderId, Long menuItemId, int rating, String comment)
            throws SQLException {
        String sql = """
                INSERT INTO reviews (user_id, order_id, menu_item_id, rating, comment)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setLong(2, orderId);
            if (menuItemId == null) {
                ps.setNull(3, Types.BIGINT);
            } else {
                ps.setLong(3, menuItemId);
            }
            ps.setInt(4, rating);
            if (comment == null || comment.isBlank()) {
                ps.setNull(5, Types.LONGVARCHAR);
            } else {
                ps.setString(5, comment.trim());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Không lấy được id review.");
    }

    public List<DishOption> listDishOptionsForPaidOrder(Connection c, long customerUserId, long orderId)
            throws SQLException {
        String sql = """
                SELECT DISTINCT od.menu_item_id, mi.name
                FROM order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                WHERE o.id = ? AND o.customer_user_id = ? AND o.status = 'PAID'
                  AND od.line_status <> 'CANCELLED'
                ORDER BY mi.name ASC, od.menu_item_id ASC
                """;
        List<DishOption> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, customerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new DishOption(rs.getLong("menu_item_id"), rs.getString("name")));
                }
            }
        }
        return out;
    }

    public boolean isMenuItemInPaidOrder(Connection c, long customerUserId, long orderId, long menuItemId)
            throws SQLException {
        String sql = """
                SELECT 1 FROM order_details od
                INNER JOIN orders o ON o.id = od.order_id
                WHERE o.id = ? AND o.customer_user_id = ? AND o.status = 'PAID'
                  AND od.menu_item_id = ? AND od.line_status <> 'CANCELLED'
                LIMIT 1
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, customerUserId);
            ps.setLong(3, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<ReviewListRow> listAllForManager() throws SQLException {
        String sql = """
                SELECT r.id, u.username, r.order_id, mi.name AS menu_name, r.rating, r.comment, r.created_at
                FROM reviews r
                INNER JOIN users u ON u.id = r.user_id
                LEFT JOIN menu_items mi ON mi.id = r.menu_item_id
                ORDER BY r.created_at DESC, r.id DESC
                """;
        List<ReviewListRow> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ReviewListRow row = new ReviewListRow();
                row.setId(rs.getLong("id"));
                row.setUsername(rs.getString("username"));
                long oid = rs.getLong("order_id");
                row.setOrderId(rs.wasNull() ? null : oid);
                row.setMenuItemName(rs.getString("menu_name"));
                row.setRating(rs.getInt("rating"));
                row.setComment(rs.getString("comment"));
                row.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                out.add(row);
            }
        }
        return out;
    }

    public Optional<Long> findPaidOrderIdForCustomer(Connection c, long customerUserId, long orderId)
            throws SQLException {
        String sql = """
                SELECT id FROM orders
                WHERE id = ? AND customer_user_id = ? AND status = 'PAID'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, customerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getLong("id"));
                }
            }
        }
        return Optional.empty();
    }
}
