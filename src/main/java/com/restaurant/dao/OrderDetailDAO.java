package com.restaurant.dao;

import com.restaurant.model.ChefKitchenLine;
import com.restaurant.model.ManagerApproval;
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

    private static final String LINE_VIEW_SELECT = """
            od.id, od.menu_item_id, mi.name, mi.item_type, od.quantity, od.unit_price, od.line_status,
            od.manager_approval
            """;

    private static OrderLineView mapLineView(ResultSet rs) throws SQLException {
        OrderLineView v = new OrderLineView();
        v.setDetailId(rs.getLong("id"));
        v.setMenuItemId(rs.getLong("menu_item_id"));
        v.setMenuItemName(rs.getString("name"));
        v.setItemType(MenuItemType.valueOf(rs.getString("item_type")));
        v.setQuantity(rs.getInt("quantity"));
        v.setUnitPrice(rs.getBigDecimal("unit_price"));
        v.setLineStatus(OrderLineStatus.valueOf(rs.getString("line_status")));
        v.setManagerApproval(ManagerApproval.valueOf(rs.getString("manager_approval")));
        return v;
    }

    public List<OrderLineView> listLinesForCustomerOrder(long orderId, long customerUserId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return listLinesForCustomerOrder(c, orderId, customerUserId);
        }
    }

    /** Chi tiết order (không lọc khách) — dùng thanh toán / quản lý. */
    public List<OrderLineView> listLinesForOrder(long orderId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return listLinesForOrder(c, orderId);
        }
    }

    public List<OrderLineView> listLinesForOrder(Connection c, long orderId) throws SQLException {
        String sql = """
                SELECT
                """ + LINE_VIEW_SELECT + """
                FROM order_details od
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                WHERE od.order_id = ?
                ORDER BY od.created_at ASC, od.id ASC
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderLineView> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapLineView(rs));
                }
                return list;
            }
        }
    }

    public List<OrderLineView> listLinesForCustomerOrder(Connection c, long orderId, long customerUserId)
            throws SQLException {
        String sql = """
                SELECT
                """ + LINE_VIEW_SELECT + """
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
                INSERT INTO order_details (order_id, menu_item_id, quantity, unit_price, line_status, manager_approval)
                VALUES (?, ?, ?, ?, 'PENDING', 'PENDING')
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
     * Dòng PENDING của khách; chỉ khi chưa được quản lý duyệt (đồ uống chưa trừ kho).
     */
    public Optional<PendingLineInfo> findPendingLineForCustomer(Connection c, long detailId, long customerUserId)
            throws SQLException {
        String sql = """
                SELECT od.order_id, od.menu_item_id, od.quantity, mi.item_type
                FROM order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                WHERE od.id = ? AND o.customer_user_id = ? AND od.line_status = 'PENDING'
                  AND od.manager_approval = 'PENDING'
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
     * Nâng cao 08 — đồ ăn: bếp nấu khi quản lý chưa duyệt; đồ uống: chỉ sau khi duyệt (trừ kho).
     * Đồ ăn READY + chưa duyệt: vẫn hiện ở bếp (chờ quản lý duyệt mới được READY→SERVED).
     */
    public List<ChefKitchenLine> listKitchenQueue() throws SQLException {
        String sql = """
                SELECT od.id, o.id AS order_id, dt.table_code, mi.name, mi.item_type,
                       od.quantity, od.line_status, od.manager_approval, od.created_at
                FROM order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN dining_tables dt ON dt.id = o.table_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                WHERE o.status = 'OPEN'
                  AND od.line_status IN ('PENDING', 'COOKING', 'READY')
                  AND (
                    (mi.item_type = 'FOOD' AND od.manager_approval = 'PENDING')
                    OR (mi.item_type = 'FOOD' AND od.manager_approval = 'APPROVED' AND od.line_status = 'READY')
                    OR (mi.item_type = 'DRINK' AND od.manager_approval = 'APPROVED')
                  )
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

    /** Một dòng order OPEN (bếp / kiểm tra bước tiếp theo). */
    public Optional<ChefKitchenLine> findOpenKitchenLine(long detailId) throws SQLException {
        String sql = """
                SELECT od.id, o.id AS order_id, dt.table_code, mi.name, mi.item_type,
                       od.quantity, od.line_status, od.manager_approval, od.created_at
                FROM order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN dining_tables dt ON dt.id = o.table_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                WHERE od.id = ? AND o.status = 'OPEN'
                  AND od.line_status NOT IN ('CANCELLED', 'SERVED')
                  AND od.manager_approval <> 'REJECTED'
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

    /** Đồ ăn: PENDING + quản lý chưa duyệt — chuyển PENDING→COOKING→READY. */
    public int advanceFoodCooking(long detailId, OrderLineStatus from, OrderLineStatus to) throws SQLException {
        String sql = """
                UPDATE order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                SET od.line_status = ?
                WHERE od.id = ? AND o.status = 'OPEN'
                  AND mi.item_type = 'FOOD'
                  AND od.manager_approval = 'PENDING'
                  AND od.line_status = ?
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, to.name());
            ps.setLong(2, detailId);
            ps.setString(3, from.name());
            return ps.executeUpdate();
        }
    }

    /** Đồ ăn: READY + đã duyệt — phục vụ. */
    public int advanceFoodReadyToServed(long detailId) throws SQLException {
        String sql = """
                UPDATE order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                SET od.line_status = 'SERVED'
                WHERE od.id = ? AND o.status = 'OPEN'
                  AND mi.item_type = 'FOOD'
                  AND od.manager_approval = 'APPROVED'
                  AND od.line_status = 'READY'
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, detailId);
            return ps.executeUpdate();
        }
    }

    /** Đồ uống: đã duyệt quản lý — chuyển bước bếp/phục vụ. */
    public int advanceDrinkLine(long detailId, OrderLineStatus from, OrderLineStatus to) throws SQLException {
        String sql = """
                UPDATE order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                SET od.line_status = ?
                WHERE od.id = ? AND o.status = 'OPEN'
                  AND mi.item_type = 'DRINK'
                  AND od.manager_approval = 'APPROVED'
                  AND od.line_status = ?
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, to.name());
            ps.setLong(2, detailId);
            ps.setString(3, from.name());
            return ps.executeUpdate();
        }
    }

    public Optional<DetailApprovalRow> findDetailApprovalRow(Connection c, long detailId) throws SQLException {
        String sql = """
                SELECT od.order_id, od.menu_item_id, mi.item_type, od.quantity, od.line_status, od.manager_approval
                FROM order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                WHERE od.id = ? AND o.status = 'OPEN'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, detailId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new DetailApprovalRow(
                        rs.getLong("order_id"),
                        rs.getLong("menu_item_id"),
                        MenuItemType.valueOf(rs.getString("item_type")),
                        rs.getInt("quantity"),
                        OrderLineStatus.valueOf(rs.getString("line_status")),
                        ManagerApproval.valueOf(rs.getString("manager_approval"))));
            }
        }
    }

    public Optional<DetailApprovalRow> findDetailApprovalRow(long detailId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return findDetailApprovalRow(c, detailId);
        }
    }

    /** Đồ ăn: duyệt sau khi bếp READY. */
    public int approveFoodIfReadyPending(Connection c, long detailId) throws SQLException {
        String sql = """
                UPDATE order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                SET od.manager_approval = 'APPROVED'
                WHERE od.id = ? AND o.status = 'OPEN'
                  AND mi.item_type = 'FOOD'
                  AND od.line_status = 'READY'
                  AND od.manager_approval = 'PENDING'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, detailId);
            return ps.executeUpdate();
        }
    }

    /** Đồ uống: duyệt khi còn PENDING dòng + chờ quản lý (kho trừ ở service). */
    public int approveDrinkIfPending(Connection c, long detailId) throws SQLException {
        String sql = """
                UPDATE order_details od
                INNER JOIN orders o ON o.id = od.order_id
                INNER JOIN menu_items mi ON mi.id = od.menu_item_id
                SET od.manager_approval = 'APPROVED'
                WHERE od.id = ? AND o.status = 'OPEN'
                  AND mi.item_type = 'DRINK'
                  AND od.line_status = 'PENDING'
                  AND od.manager_approval = 'PENDING'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, detailId);
            return ps.executeUpdate();
        }
    }

    public int rejectIfManagerPending(Connection c, long detailId) throws SQLException {
        String sql = """
                UPDATE order_details od
                INNER JOIN orders o ON o.id = od.order_id
                SET od.manager_approval = 'REJECTED',
                    od.line_status = 'CANCELLED'
                WHERE od.id = ? AND o.status = 'OPEN'
                  AND od.manager_approval = 'PENDING'
                  AND od.line_status NOT IN ('SERVED', 'CANCELLED')
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, detailId);
            return ps.executeUpdate();
        }
    }

    private static ChefKitchenLine mapChefLine(ResultSet rs) throws SQLException {
        ChefKitchenLine row = new ChefKitchenLine();
        row.setDetailId(rs.getLong("id"));
        row.setOrderId(rs.getLong("order_id"));
        row.setTableCode(rs.getString("table_code"));
        row.setMenuItemName(rs.getString("name"));
        row.setItemType(MenuItemType.valueOf(rs.getString("item_type")));
        row.setQuantity(rs.getInt("quantity"));
        row.setLineStatus(OrderLineStatus.valueOf(rs.getString("line_status")));
        row.setManagerApproval(ManagerApproval.valueOf(rs.getString("manager_approval")));
        row.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return row;
    }

    public int markCancelledIfPending(Connection c, long detailId, long customerUserId) throws SQLException {
        String sql = """
                UPDATE order_details od
                INNER JOIN orders o ON o.id = od.order_id
                SET od.line_status = 'CANCELLED'
                WHERE od.id = ? AND o.customer_user_id = ? AND od.line_status = 'PENDING'
                  AND od.manager_approval = 'PENDING'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, detailId);
            ps.setLong(2, customerUserId);
            return ps.executeUpdate();
        }
    }

    public record DetailApprovalRow(
            long orderId,
            long menuItemId,
            MenuItemType itemType,
            int quantity,
            OrderLineStatus lineStatus,
            ManagerApproval managerApproval
    ) {
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
