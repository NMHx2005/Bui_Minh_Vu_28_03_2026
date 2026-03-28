package com.restaurant.service;

import com.restaurant.dao.DiningTableDAO;
import com.restaurant.dao.OrderDAO;
import com.restaurant.dao.OrderDetailDAO;
import com.restaurant.model.CheckoutInvoice;
import com.restaurant.model.OrderLineStatus;
import com.restaurant.model.OrderLineView;
import com.restaurant.model.RestaurantOrder;
import com.restaurant.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Thanh toán: chỉ khi mọi dòng không bị hủy đã ở trạng thái SERVED.
 * Khách chỉ thanh toán order OPEN của chính mình; quản lý thanh toán theo mã order OPEN bất kỳ.
 */
public class CheckoutService {

    public record OpenOrderPreview(RestaurantOrder order, List<OrderLineView> lines) {
    }

    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderDetailDAO orderDetailDAO = new OrderDetailDAO();
    private final DiningTableDAO diningTableDAO = new DiningTableDAO();

    public CheckoutInvoice checkoutForCustomer(long customerUserId) throws ServiceException {
        try {
            Optional<RestaurantOrder> orderOpt = orderDAO.findOpenOrderForCustomer(customerUserId);
            if (orderOpt.isEmpty()) {
                throw new ServiceException("Bạn không có order đang mở để thanh toán.");
            }
            return checkoutOrderInTransaction(orderOpt.get().getId());
        } catch (SQLException e) {
            throw new ServiceException("Lỗi CSDL khi thanh toán.", e);
        }
    }

    public OpenOrderPreview loadOpenOrderPreview(long orderId) throws ServiceException {
        try {
            Optional<RestaurantOrder> o = orderDAO.findOpenOrderById(orderId);
            if (o.isEmpty()) {
                throw new ServiceException("Không có order OPEN với id = " + orderId + ".");
            }
            List<OrderLineView> lines = orderDetailDAO.listLinesForOrder(orderId);
            return new OpenOrderPreview(o.get(), lines);
        } catch (SQLException e) {
            throw new ServiceException("Không tải được order.", e);
        }
    }

    public CheckoutInvoice checkoutAsManager(long orderId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            Optional<RestaurantOrder> orderOpt = orderDAO.findOpenById(conn, orderId);
            if (orderOpt.isEmpty()) {
                throw new ServiceException("Không tìm thấy order OPEN có id = " + orderId + ".");
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi CSDL.", e);
        }
        return checkoutOrderInTransaction(orderId);
    }

    private CheckoutInvoice checkoutOrderInTransaction(long orderId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Optional<RestaurantOrder> orderOpt = orderDAO.findOpenById(conn, orderId);
                if (orderOpt.isEmpty()) {
                    throw new ServiceException("Order không còn ở trạng thái OPEN.");
                }
                RestaurantOrder order = orderOpt.get();
                List<OrderLineView> lines = orderDetailDAO.listLinesForOrder(conn, orderId);
                assertReadyForCheckout(lines);

                int paid = orderDAO.markOrderPaidIfOpen(conn, orderId);
                if (paid != 1) {
                    throw new ServiceException("Không thể xác nhận thanh toán (order đã đóng hoặc không tồn tại).");
                }
                diningTableDAO.forceFree(conn, order.getTableId());
                conn.commit();
            } catch (ServiceException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new ServiceException("Thanh toán thất bại.", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi kết nối CSDL.", e);
        }
        return buildInvoice(orderId);
    }

    private static void assertReadyForCheckout(List<OrderLineView> lines) throws ServiceException {
        if (lines.isEmpty()) {
            throw new ServiceException("Order không có dòng nào — không thể thanh toán.");
        }
        boolean anyBillable = false;
        for (OrderLineView v : lines) {
            if (v.getLineStatus() == OrderLineStatus.CANCELLED) {
                continue;
            }
            anyBillable = true;
            if (v.getLineStatus() != OrderLineStatus.SERVED) {
                throw new ServiceException(
                        "Chưa thể thanh toán: còn món chưa phục vụ xong (món \"" + v.getMenuItemName()
                                + "\" đang: " + orderLineStatusLabel(v.getLineStatus())
                                + "). Quy ước: tất cả món (trừ đã hủy) phải ở trạng thái SERVED.");
            }
        }
        if (!anyBillable) {
            throw new ServiceException("Tất cả món đã hủy — không có khoản phải trả. Liên hệ quản lý để đóng phiên.");
        }
    }

    private static String orderLineStatusLabel(OrderLineStatus s) {
        return switch (s) {
            case PENDING -> "Chờ";
            case COOKING -> "Đang nấu";
            case READY -> "Sẵn sàng";
            case SERVED -> "Đã phục vụ";
            case CANCELLED -> "Đã hủy";
        };
    }

    private CheckoutInvoice buildInvoice(long orderId) throws ServiceException {
        try {
            RestaurantOrder o = orderDAO.findById(orderId)
                    .orElseThrow(() -> new ServiceException("Không đọc lại được order sau thanh toán."));
            if (o.getCheckedOutAt() == null) {
                throw new ServiceException("Dữ liệu checkout không đầy đủ.");
            }
            var table = diningTableDAO.findById(o.getTableId())
                    .orElseThrow(() -> new ServiceException("Không đọc được thông tin bàn."));
            List<OrderLineView> all = orderDetailDAO.listLinesForOrder(orderId);
            List<OrderLineView> billable = all.stream()
                    .filter(l -> l.getLineStatus() != OrderLineStatus.CANCELLED)
                    .collect(Collectors.toCollection(ArrayList::new));
            CheckoutInvoice inv = new CheckoutInvoice();
            inv.setOrderId(o.getId());
            inv.setTableCode(table.getTableCode());
            inv.setCheckedOutAt(o.getCheckedOutAt());
            inv.setTotalAmount(o.getTotalAmount());
            inv.setBillableLines(billable);
            return inv;
        } catch (SQLException e) {
            throw new ServiceException("Lỗi khi tạo hóa đơn.", e);
        }
    }
}
