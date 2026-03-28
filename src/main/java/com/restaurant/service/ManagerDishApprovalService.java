package com.restaurant.service;

import com.restaurant.dao.MenuItemDAO;
import com.restaurant.dao.OrderDAO;
import com.restaurant.dao.OrderDetailDAO;
import com.restaurant.model.ManagerApproval;
import com.restaurant.model.MenuItemType;
import com.restaurant.model.OrderLineStatus;
import com.restaurant.model.OrderLineView;
import com.restaurant.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Duyệt / từ chối món (08): đồ ăn sau READY; đồ uống trước bếp + trừ kho khi duyệt.
 */
public class ManagerDishApprovalService {

    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderDetailDAO orderDetailDAO = new OrderDetailDAO();
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();

    public List<OrderDAO.OpenOrderSummary> listOpenOrders() throws ServiceException {
        try {
            return orderDAO.listOpenOrderSummaries();
        } catch (SQLException e) {
            throw new ServiceException("Không tải được danh sách order đang mở.", e);
        }
    }

    public List<OrderLineView> listLinesForOrder(long orderId) throws ServiceException {
        try {
            return orderDetailDAO.listLinesForOrder(orderId);
        } catch (SQLException e) {
            throw new ServiceException("Không tải được chi tiết order.", e);
        }
    }

    public void approveLine(long detailId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                OrderDetailDAO.DetailApprovalRow row = orderDetailDAO.findDetailApprovalRow(conn, detailId)
                        .orElseThrow(() -> new ServiceException("Không tìm thấy dòng hoặc order không còn OPEN."));
                if (row.managerApproval() != ManagerApproval.PENDING) {
                    throw new ServiceException("Dòng này không ở trạng thái chờ duyệt (PENDING).");
                }
                if (row.itemType() == MenuItemType.FOOD) {
                    if (row.lineStatus() != OrderLineStatus.READY) {
                        throw new ServiceException(
                                "Đồ ăn chỉ duyệt khi bếp đã nấu xong (trạng thái READY). Hiện tại: "
                                        + lineStatusLabel(row.lineStatus()) + ".");
                    }
                    if (orderDetailDAO.approveFoodIfReadyPending(conn, detailId) != 1) {
                        throw new ServiceException("Không cập nhật được duyệt đồ ăn (trạng thái có thể đã đổi).");
                    }
                } else {
                    if (row.lineStatus() != OrderLineStatus.PENDING) {
                        throw new ServiceException(
                                "Đồ uống chỉ duyệt khi dòng đang PENDING. Hiện tại: "
                                        + lineStatusLabel(row.lineStatus()) + ".");
                    }
                    if (!menuItemDAO.decrementDrinkStock(conn, row.menuItemId(), row.quantity())) {
                        throw new ServiceException(
                                "Không đủ tồn kho đồ uống cho số lượng đã gọi — không thể duyệt.");
                    }
                    if (orderDetailDAO.approveDrinkIfPending(conn, detailId) != 1) {
                        throw new ServiceException("Không cập nhật được duyệt đồ uống (trạng thái có thể đã đổi).");
                    }
                }
                orderDAO.recalculateTotalAmount(conn, row.orderId());
                conn.commit();
            } catch (ServiceException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new ServiceException("Lỗi khi duyệt món.", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi kết nối CSDL.", e);
        }
    }

    public void rejectLine(long detailId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                OrderDetailDAO.DetailApprovalRow row = orderDetailDAO.findDetailApprovalRow(conn, detailId)
                        .orElseThrow(() -> new ServiceException("Không tìm thấy dòng hoặc order không còn OPEN."));
                if (row.managerApproval() != ManagerApproval.PENDING) {
                    throw new ServiceException("Chỉ từ chối được dòng đang chờ duyệt (PENDING).");
                }
                if (row.lineStatus() == OrderLineStatus.SERVED || row.lineStatus() == OrderLineStatus.CANCELLED) {
                    throw new ServiceException("Không thể từ chối dòng đã SERVED hoặc CANCELLED.");
                }
                int n = orderDetailDAO.rejectIfManagerPending(conn, detailId);
                if (n != 1) {
                    throw new ServiceException("Không từ chối được (trạng thái có thể đã đổi).");
                }
                orderDAO.recalculateTotalAmount(conn, row.orderId());
                conn.commit();
            } catch (ServiceException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new ServiceException("Lỗi khi từ chối món.", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi kết nối CSDL.", e);
        }
    }

    /**
     * Thử duyệt từng dòng đủ điều kiện trong order; gom thông báo lỗi từng dòng.
     *
     * @return danh sách lỗi (rỗng nếu mọi dòng đều OK hoặc không có dòng nào thử)
     */
    public List<String> batchApproveOrder(long orderId) throws ServiceException {
        List<OrderLineView> lines;
        try {
            lines = orderDetailDAO.listLinesForOrder(orderId);
        } catch (SQLException e) {
            throw new ServiceException("Không đọc được chi tiết order.", e);
        }
        List<String> errors = new ArrayList<>();
        int attempted = 0;
        for (OrderLineView v : lines) {
            if (v.getManagerApproval() != ManagerApproval.PENDING) {
                continue;
            }
            attempted++;
            try {
                approveLine(v.getDetailId());
            } catch (ServiceException e) {
                errors.add("Dòng #" + v.getDetailId() + " (" + v.getMenuItemName() + "): " + e.getMessage());
            }
        }
        if (attempted == 0 && errors.isEmpty()) {
            errors.add("Không có dòng nào đang chờ duyệt (PENDING) trong order này.");
        }
        return errors;
    }

    private static String lineStatusLabel(OrderLineStatus s) {
        return switch (s) {
            case PENDING -> "Chờ";
            case COOKING -> "Đang nấu";
            case READY -> "Sẵn sàng";
            case SERVED -> "Đã phục vụ";
            case CANCELLED -> "Đã hủy";
        };
    }
}
