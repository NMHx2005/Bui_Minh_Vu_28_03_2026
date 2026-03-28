package com.restaurant.service;

import com.restaurant.dao.OrderDetailDAO;
import com.restaurant.model.ChefKitchenLine;
import com.restaurant.model.ManagerApproval;
import com.restaurant.model.MenuItemType;
import com.restaurant.model.OrderLineStatus;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ChefService {

    private final OrderDetailDAO orderDetailDAO = new OrderDetailDAO();

    public List<ChefKitchenLine> listKitchenQueue() throws ServiceException {
        try {
            return orderDetailDAO.listKitchenQueue();
        } catch (SQLException e) {
            throw new ServiceException("Không tải được hàng đợi bếp.", e);
        }
    }

    /**
     * Đồ ăn: nấu khi quản lý chưa duyệt; READY→SERVED chỉ khi đã duyệt.
     * Đồ uống: chỉ xử lý sau khi quản lý duyệt (đã trừ kho).
     */
    public void advanceLineStatus(long orderDetailId) throws ServiceException {
        try {
            Optional<ChefKitchenLine> opt = orderDetailDAO.findOpenKitchenLine(orderDetailId);
            if (opt.isEmpty()) {
                throw new ServiceException("Không tìm thấy dòng, order đã đóng, hoặc món đã xử lý xong.");
            }
            ChefKitchenLine line = opt.get();
            OrderLineStatus cur = line.getLineStatus();
            if (cur == OrderLineStatus.CANCELLED) {
                throw new ServiceException("Dòng đã hủy — không cập nhật được.");
            }
            if (cur == OrderLineStatus.SERVED) {
                throw new ServiceException("Món đã phục vụ — không cần cập nhật thêm.");
            }
            MenuItemType type = line.getItemType();
            ManagerApproval ma = line.getManagerApproval();

            if (type == MenuItemType.DRINK && ma != ManagerApproval.APPROVED) {
                throw new ServiceException("Đồ uống chưa được quản lý duyệt — không thể xử lý ở bếp.");
            }

            if (type == MenuItemType.FOOD && cur == OrderLineStatus.READY && ma == ManagerApproval.PENDING) {
                throw new ServiceException(
                        "Đồ ăn đã nấu xong (READY) — chờ quản lý duyệt trước khi chuyển sang Đã phục vụ (SERVED).");
            }

            Optional<OrderLineStatus> next = nextStep(cur);
            if (next.isEmpty()) {
                throw new ServiceException("Trạng thái hiện tại không thể chuyển bước tiếp theo.");
            }

            int updated;
            if (type == MenuItemType.FOOD) {
                if (cur == OrderLineStatus.READY && ma == ManagerApproval.APPROVED) {
                    updated = orderDetailDAO.advanceFoodReadyToServed(orderDetailId);
                } else {
                    updated = orderDetailDAO.advanceFoodCooking(orderDetailId, cur, next.get());
                }
            } else {
                updated = orderDetailDAO.advanceDrinkLine(orderDetailId, cur, next.get());
            }
            if (updated != 1) {
                throw new ServiceException("Cập nhật thất bại (trạng thái có thể đã đổi bởi thao tác khác).");
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi CSDL khi cập nhật trạng thái.", e);
        }
    }

    private static Optional<OrderLineStatus> nextStep(OrderLineStatus current) {
        return switch (current) {
            case PENDING -> Optional.of(OrderLineStatus.COOKING);
            case COOKING -> Optional.of(OrderLineStatus.READY);
            case READY -> Optional.of(OrderLineStatus.SERVED);
            default -> Optional.empty();
        };
    }
}
