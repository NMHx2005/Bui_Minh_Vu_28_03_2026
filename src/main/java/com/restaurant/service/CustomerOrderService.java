package com.restaurant.service;

import com.restaurant.dao.DiningTableDAO;
import com.restaurant.dao.MenuItemDAO;
import com.restaurant.dao.OrderDAO;
import com.restaurant.dao.OrderDetailDAO;
import com.restaurant.model.DiningTable;
import com.restaurant.model.MenuItem;
import com.restaurant.model.MenuItemType;
import com.restaurant.model.OrderLineView;
import com.restaurant.model.RestaurantOrder;
import com.restaurant.model.TableStatus;
import com.restaurant.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CustomerOrderService {

    private static final int QTY_MIN = 1;
    private static final int QTY_MAX = 999;

    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderDetailDAO orderDetailDAO = new OrderDetailDAO();
    private final DiningTableDAO diningTableDAO = new DiningTableDAO();
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();

    public List<MenuItem> listActiveMenu() throws ServiceException {
        try {
            return menuItemDAO.findAllActive();
        } catch (SQLException e) {
            throw new ServiceException("Không tải được thực đơn.", e);
        }
    }

    public List<DiningTable> listFreeTables() throws ServiceException {
        try {
            return diningTableDAO.findByStatus(TableStatus.FREE);
        } catch (SQLException e) {
            throw new ServiceException("Không tải được danh sách bàn.", e);
        }
    }

    public Optional<RestaurantOrder> getOpenOrder(long customerUserId) throws ServiceException {
        try {
            return orderDAO.findOpenOrderForCustomer(customerUserId);
        } catch (SQLException e) {
            throw new ServiceException("Không đọc được phiên order.", e);
        }
    }

    public Optional<DiningTable> getTable(long tableId) throws ServiceException {
        try {
            return diningTableDAO.findById(tableId);
        } catch (SQLException e) {
            throw new ServiceException("Không đọc được thông tin bàn.", e);
        }
    }

    /**
     * Chọn bàn trống, tạo order OPEN, đánh dấu bàn OCCUPIED (một transaction).
     */
    public long startSessionAtTable(long customerUserId, long tableId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (orderDAO.findOpenOrderForCustomer(conn, customerUserId).isPresent()) {
                    throw new ServiceException("Bạn đang có phiên order đang mở. Hãy gọi món hoặc hoàn tất trước khi chọn bàn khác.");
                }
                Optional<DiningTable> tableOpt = diningTableDAO.findById(conn, tableId);
                if (tableOpt.isEmpty()) {
                    throw new ServiceException("Không tìm thấy bàn.");
                }
                if (tableOpt.get().getStatus() != TableStatus.FREE) {
                    throw new ServiceException("Bàn này đang OCCUPIED (đang có khách). Chỉ được chọn bàn có trạng thái Trống trong danh sách mục 2.");
                }
                int updated = diningTableDAO.updateStatusIf(conn, tableId, TableStatus.FREE, TableStatus.OCCUPIED);
                if (updated != 1) {
                    throw new ServiceException("Bàn vừa được người khác chọn. Vui lòng chọn bàn khác.");
                }
                long orderId = orderDAO.insertOpenOrder(conn, tableId, customerUserId);
                conn.commit();
                return orderId;
            } catch (ServiceException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new ServiceException("Không tạo được phiên tại bàn.", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi kết nối CSDL.", e);
        }
    }

    public void addOrderLine(long customerUserId, long menuItemId, int quantity) throws ServiceException {
        if (quantity < QTY_MIN || quantity > QTY_MAX) {
            throw new ServiceException("Số lượng phải từ " + QTY_MIN + " đến " + QTY_MAX + ".");
        }
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Optional<RestaurantOrder> orderOpt = orderDAO.findOpenOrderForCustomer(conn, customerUserId);
                if (orderOpt.isEmpty()) {
                    throw new ServiceException("Bạn chưa chọn bàn. Vui lòng chọn bàn trống trước khi gọi món.");
                }
                RestaurantOrder order = orderOpt.get();
                Optional<MenuItem> itemOpt = menuItemDAO.findById(conn, menuItemId);
                if (itemOpt.isEmpty() || !itemOpt.get().isActive()) {
                    throw new ServiceException("Món không tồn tại hoặc đã ngừng phục vụ.");
                }
                MenuItem item = itemOpt.get();
                if (item.getItemType() == MenuItemType.DRINK) {
                    Integer stock = item.getStockQuantity();
                    if (stock == null || stock < quantity) {
                        throw new ServiceException("Không đủ tồn kho đồ uống cho số lượng bạn chọn (chỉ kiểm tra; trừ kho khi quản lý duyệt).");
                    }
                }
                orderDetailDAO.insertLine(conn, order.getId(), menuItemId, quantity, item.getPrice());
                orderDAO.recalculateTotalAmount(conn, order.getId());
                conn.commit();
            } catch (ServiceException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new ServiceException("Không thêm được món vào order.", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi kết nối CSDL.", e);
        }
    }

    public List<OrderLineView> listMyOrderLines(long customerUserId) throws ServiceException {
        try {
            Optional<RestaurantOrder> orderOpt = orderDAO.findOpenOrderForCustomer(customerUserId);
            if (orderOpt.isEmpty()) {
                return List.of();
            }
            return orderDetailDAO.listLinesForCustomerOrder(orderOpt.get().getId(), customerUserId);
        } catch (SQLException e) {
            throw new ServiceException("Không đọc được chi tiết order.", e);
        }
    }

    public void cancelOrderLine(long customerUserId, long orderDetailId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Optional<OrderDetailDAO.PendingLineInfo> infoOpt =
                        orderDetailDAO.findPendingLineForCustomer(conn, orderDetailId, customerUserId);
                if (infoOpt.isEmpty()) {
                    throw new ServiceException("Không hủy được: dòng không tồn tại, không thuộc order của bạn, hoặc món đã chuyển khỏi trạng thái PENDING.");
                }
                OrderDetailDAO.PendingLineInfo info = infoOpt.get();
                int n = orderDetailDAO.markCancelledIfPending(conn, orderDetailId, customerUserId);
                if (n != 1) {
                    throw new ServiceException("Không hủy được (trạng thái đã thay đổi).");
                }
                orderDAO.recalculateTotalAmount(conn, info.getOrderId());
                conn.commit();
            } catch (ServiceException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new ServiceException("Lỗi khi hủy món.", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi kết nối CSDL.", e);
        }
    }
}
