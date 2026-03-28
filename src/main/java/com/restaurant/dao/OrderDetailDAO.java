package com.restaurant.dao;

import com.restaurant.model.ChefKitchenLine;
import com.restaurant.model.MenuItemType;
import com.restaurant.model.OrderLineStatus;
import com.restaurant.model.OrderLineView;
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

public class OrderDetailDAO {

    private static OrderLineView mapLineView(ResultSet rs) throws SQLException {
        OrderLineView v = new OrderLineView();
        v.setDetailId(rs.getLong("id"));
        v.setMenuItemId(rs.getLong("menu_item_id"));
        v.setMenuItemName(rs.getString("name"));
        v.setItemType(MenuItemType.valueOf(rs.getString("item_type")));
        v.setQuantity(rs.getInt("quantity"));
        v.setUnitPrice(rs.getBigDecimal("unit_price"));
        v.setLineStatus(OrderLineStatus.valueOf(rs.getString("line_status")));
        return v;
    }

    public List<OrderLineView> listLinesForCustomerOrder(long orderId, long customerUserId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return listLinesForCustomerOrder(c, orderId, customerUserId);
        }
    }

    public List<OrderLineView> listLinesForCustomerOrder(Connection c, long orderId, long customerUserId)
            throws SQLException {
        String sql = """
                SELECT od.id, od.menu_item_id, mi.name, mi.item_type, od.quantity, od.unit_price, od.line_status
                FROM order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                WHERE od.order_id = ? AND o.customer_user_id = ?
                ORDER BY od.created_at ASC, od.id ASC
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, customerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderLineView> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapLineView(rs));
                }
                return list;
            }
        }
    }

    public long insertLine(Connection c, long orderId, long menuItemId, int quantity, BigDecimal unitPrice)
            throws SQLException {
        String sql = """
                INSERT INTO order_details (order_id, menu_item_id, quantity, unit_price, line_status)
                VALUES (?, ?, ?, ?, 'PENDING')
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, orderId);
            ps.setLong(2, menuItemId);
            ps.setInt(3, quantity);
            ps.setBigDecimal(4, unitPrice);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Không lấy được id chi tiết order.");
    }

    /**
     * Dòng PENDING của khách; dùng trước khi hủy để biết loại món (hoàn kho đồ uống).
     */
    public Optional<PendingLineInfo> findPendingLineForCustomer(Connection c, long detailId, long customerUserId)
            throws SQLException {
        String sql = """
                SELECT od.order_id, od.menu_item_id, od.quantity, mi.item_type
                FROM order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                WHERE od.id = ? AND o.customer_user_id = ? AND od.line_status = 'PENDING'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, detailId);
            ps.setLong(2, customerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new PendingLineInfo(
                        rs.getLong("order_id"),
                        rs.getLong("menu_item_id"),
                        rs.getInt("quantity"),
                        MenuItemType.valueOf(rs.getString("item_type"))));
            }
        }
    }

    /**
     * Hàng đợi bếp: order đang OPEN, đã duyệt (nâng cao 08), chưa SERVED/CANCELLED.
     * Sắp xếp theo thời gian tạo dòng tăng dần.
     */
    public List<ChefKitchenLine> listKitchenQueue() throws SQLException {
        String sql = """
                SELECT od.id, o.id AS order_id, dt.table_code, mi.name, od.quantity, od.line_status, od.created_at
                FROM order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN dining_tables dt ON dt.id = o.table_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                WHERE o.status = 'OPEN'
                  AND od.manager_approval = 'APPROVED'
                  AND od.line_status IN ('PENDING', 'COOKING', 'READY')
                ORDER BY od.created_at ASC, od.id ASC
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ChefKitchenLine> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapChefLine(rs));
            }
            return list;
        }
    }

    /**
     * Dòng thuộc order OPEN và đã APPROVED (để đầu bếp cập nhật / báo lỗi rõ).
     */
    public Optional<ChefKitchenLine> findKitchenLineForChef(long detailId) throws SQLException {
        String sql = """
                SELECT od.id, o.id AS order_id, dt.table_code, mi.name, od.quantity, od.line_status, od.created_at
                FROM order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN dining_tables dt ON dt.id = o.table_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                WHERE od.id = ?
                  AND o.status = 'OPEN'
                  AND od.manager_approval = 'APPROVED'
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, detailId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapChefLine(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Chỉ cập nhật nếu trạng thái hiện tại đúng {@code expected} (tránh race đơn giản).
     */
    public int updateLineStatusIf(long detailId, OrderLineStatus expected, OrderLineStatus next) throws SQLException {
        String sql = """
                UPDATE order_details od
                INNER JOIN orders o ON o.id = od.order_id
                SET od.line_status = ?
                WHERE od.id = ? AND od.line_status = ? AND o.status = 'OPEN'
                  AND od.manager_approval = 'APPROVED'
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, next.name());
            ps.setLong(2, detailId);
            ps.setString(3, expected.name());
            return ps.executeUpdate();
        }
    }

    private static ChefKitchenLine mapChefLine(ResultSet rs) throws SQLException {
        ChefKitchenLine row = new ChefKitchenLine();
        row.setDetailId(rs.getLong("id"));
        row.setOrderId(rs.getLong("order_id"));
        row.setTableCode(rs.getString("table_code"));
        row.setMenuItemName(rs.getString("name"));
        row.setQuantity(rs.getInt("quantity"));
        row.setLineStatus(OrderLineStatus.valueOf(rs.getString("line_status")));
        row.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return row;
    }

    public int markCancelledIfPending(Connection c, long detailId, long customerUserId) throws SQLException {
        String sql = """
                UPDATE order_details od
                INNER JOIN orders o ON o.id = od.order_id
                SET od.line_status = 'CANCELLED'
                WHERE od.id = ? AND o.customer_user_id = ? AND od.line_status = 'PENDING'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, detailId);
            ps.setLong(2, customerUserId);
            return ps.executeUpdate();
        }
    }

    public static class PendingLineInfo {
        private final long orderId;
        private final long menuItemId;
        private final int quantity;
        private final MenuItemType itemType;

        public PendingLineInfo(long orderId, long menuItemId, int quantity, MenuItemType itemType) {
            this.orderId = orderId;
            this.menuItemId = menuItemId;
            this.quantity = quantity;
            this.itemType = itemType;
        }

        public long getOrderId() {
            return orderId;
        }

        public long getMenuItemId() {
            return menuItemId;
        }

        public int getQuantity() {
            return quantity;
        }

        public MenuItemType getItemType() {
            return itemType;
        }
    }
}
