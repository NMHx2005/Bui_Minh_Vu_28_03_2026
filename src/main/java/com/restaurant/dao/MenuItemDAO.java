package com.restaurant.dao;

import com.restaurant.model.MenuItem;
import com.restaurant.model.MenuItemType;
import com.restaurant.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MenuItemDAO {

    private static MenuItem mapRow(ResultSet rs) throws SQLException {
        MenuItem m = new MenuItem();
        m.setId(rs.getLong("id"));
        m.setName(rs.getString("name"));
        m.setItemType(MenuItemType.valueOf(rs.getString("item_type")));
        m.setPrice(rs.getBigDecimal("price"));
        int sq = rs.getInt("stock_quantity");
        if (rs.wasNull()) {
            m.setStockQuantity(null);
        } else {
            m.setStockQuantity(sq);
        }
        m.setActive(rs.getInt("is_active") == 1);
        m.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return m;
    }

    /** Món đang bán (khách hàng). */
    public List<MenuItem> findAllActive() throws SQLException {
        String sql = """
                SELECT id, name, item_type, price, stock_quantity, is_active, created_at
                FROM menu_items
                WHERE is_active = 1
                ORDER BY item_type, id
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<MenuItem> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    public List<MenuItem> findAll() throws SQLException {
        String sql = "SELECT id, name, item_type, price, stock_quantity, is_active, created_at FROM menu_items ORDER BY id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<MenuItem> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    public Optional<MenuItem> findById(long id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return findById(c, id);
        }
    }

    public Optional<MenuItem> findById(Connection c, long id) throws SQLException {
        String sql = "SELECT id, name, item_type, price, stock_quantity, is_active, created_at FROM menu_items WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /** Trừ tồn kho đồ uống; trả false nếu không đủ hàng hoặc không phải DRINK. */
    public boolean decrementDrinkStock(Connection c, long menuItemId, int quantity) throws SQLException {
        String sql = """
                UPDATE menu_items
                SET stock_quantity = stock_quantity - ?
                WHERE id = ? AND item_type = 'DRINK' AND is_active = 1
                  AND stock_quantity IS NOT NULL AND stock_quantity >= ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setLong(2, menuItemId);
            ps.setInt(3, quantity);
            return ps.executeUpdate() > 0;
        }
    }

    public void incrementDrinkStock(Connection c, long menuItemId, int quantity) throws SQLException {
        String sql = """
                UPDATE menu_items
                SET stock_quantity = stock_quantity + ?
                WHERE id = ? AND item_type = 'DRINK' AND stock_quantity IS NOT NULL
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setLong(2, menuItemId);
            ps.executeUpdate();
        }
    }

    public List<MenuItem> searchByNameContains(String keyword) throws SQLException {
        String sql = """
                SELECT id, name, item_type, price, stock_quantity, is_active, created_at
                FROM menu_items
                WHERE LOWER(name) LIKE LOWER(CONCAT('%', ?, '%'))
                ORDER BY id
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                List<MenuItem> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    public long insert(String name, MenuItemType type, BigDecimal price, Integer stockQuantity) throws SQLException {
        String sql = "INSERT INTO menu_items (name, item_type, price, stock_quantity, is_active) VALUES (?, ?, ?, ?, 1)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, type.name());
            ps.setBigDecimal(3, price);
            if (stockQuantity == null) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, stockQuantity);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Không lấy được id món mới.");
    }

    public boolean update(long id, String name, MenuItemType type, BigDecimal price, Integer stockQuantity)
            throws SQLException {
        String sql = "UPDATE menu_items SET name = ?, item_type = ?, price = ?, stock_quantity = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, type.name());
            ps.setBigDecimal(3, price);
            if (stockQuantity == null) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, stockQuantity);
            }
            ps.setLong(5, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Ẩn món (soft delete) để tránh vỡ FK order_details. */
    public boolean softDelete(long id) throws SQLException {
        String sql = "UPDATE menu_items SET is_active = 0 WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
