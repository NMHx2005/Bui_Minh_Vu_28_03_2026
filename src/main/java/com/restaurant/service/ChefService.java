package com.restaurant.service;

import com.restaurant.dao.OrderDetailDAO;
import com.restaurant.model.ChefKitchenLine;
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
     * Chuyển một bước: PENDING→COOKING→READY→SERVED.
     */
    public void advanceLineStatus(long orderDetailId) throws ServiceException {
        try {
            Optional<ChefKitchenLine> opt = orderDetailDAO.findKitchenLineForChef(orderDetailId);
            if (opt.isEmpty()) {
                throw new ServiceException("Không tìm thấy dòng, order đã đóng, hoặc món chưa được duyệt (manager).");
            }
            ChefKitchenLine line = opt.get();
            OrderLineStatus cur = line.getLineStatus();
            if (cur == OrderLineStatus.CANCELLED) {
                throw new ServiceException("Dòng đã hủy — không cập nhật được.");
            }
            if (cur == OrderLineStatus.SERVED) {
                throw new ServiceException("Món đã phục vụ — không cần cập nhật thêm.");
            }
            Optional<OrderLineStatus> next = nextStep(cur);
            if (next.isEmpty()) {
                throw new ServiceException("Trạng thái hiện tại không thể chuyển bước tiếp theo.");
            }
            int updated = orderDetailDAO.updateLineStatusIf(orderDetailId, cur, next.get());
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
