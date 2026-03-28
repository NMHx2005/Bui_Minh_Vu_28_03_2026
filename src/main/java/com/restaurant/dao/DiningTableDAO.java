package com.restaurant.dao;

import com.restaurant.model.DiningTable;
import com.restaurant.model.TableStatus;
import com.restaurant.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DiningTableDAO {

    private static DiningTable mapRow(ResultSet rs) throws SQLException {
        DiningTable t = new DiningTable();
        t.setId(rs.getLong("id"));
        t.setTableCode(rs.getString("table_code"));
        t.setCapacity(rs.getInt("capacity"));
        t.setStatus(TableStatus.valueOf(rs.getString("status")));
        t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return t;
    }

    public List<DiningTable> findAll() throws SQLException {
        String sql = "SELECT id, table_code, capacity, status, created_at FROM dining_tables ORDER BY id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<DiningTable> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    public Optional<DiningTable> findById(long id) throws SQLException {
        String sql = "SELECT id, table_code, capacity, status, created_at FROM dining_tables WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<DiningTable> searchByCodeContains(String keyword) throws SQLException {
        String sql = """
                SELECT id, table_code, capacity, status, created_at
                FROM dining_tables
                WHERE LOWER(table_code) LIKE LOWER(CONCAT('%', ?, '%'))
                ORDER BY id
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                List<DiningTable> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    public boolean existsTableCode(String code, Long excludeId) throws SQLException {
        String sql = excludeId == null
                ? "SELECT 1 FROM dining_tables WHERE table_code = ? LIMIT 1"
                : "SELECT 1 FROM dining_tables WHERE table_code = ? AND id <> ? LIMIT 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            if (excludeId != null) {
                ps.setLong(2, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insert(String tableCode, int capacity) throws SQLException {
        String sql = "INSERT INTO dining_tables (table_code, capacity, status) VALUES (?, ?, 'FREE')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tableCode);
            ps.setInt(2, capacity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Không lấy được id bàn mới.");
    }

    public boolean update(long id, String tableCode, int capacity) throws SQLException {
        String sql = "UPDATE dining_tables SET table_code = ?, capacity = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tableCode);
            ps.setInt(2, capacity);
            ps.setLong(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int countOrdersForTable(long tableId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM orders WHERE table_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM dining_tables WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<DiningTable> findByStatus(TableStatus status) throws SQLException {
        String sql = "SELECT id, table_code, capacity, status, created_at FROM dining_tables WHERE status = ? ORDER BY id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<DiningTable> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    /**
     * Chỉ cập nhật nếu trạng thái hiện tại đúng {@code expected} (tránh race đơn giản).
     */
    public int updateStatusIf(Connection c, long tableId, TableStatus expected, TableStatus next) throws SQLException {
        String sql = "UPDATE dining_tables SET status = ? WHERE id = ? AND status = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, next.name());
            ps.setLong(2, tableId);
            ps.setString(3, expected.name());
            return ps.executeUpdate();
        }
    }

    public Optional<DiningTable> findById(Connection c, long id) throws SQLException {
        String sql = "SELECT id, table_code, capacity, status, created_at FROM dining_tables WHERE id = ?";
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

    /** Giải phóng bàn sau thanh toán (không kiểm tra trạng thái cũ). */
    public void forceFree(Connection c, long tableId) throws SQLException {
        String sql = "UPDATE dining_tables SET status = 'FREE' WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tableId);
            ps.executeUpdate();
        }
    }
}
