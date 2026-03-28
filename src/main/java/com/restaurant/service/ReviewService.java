package com.restaurant.service;

import com.restaurant.dao.OrderDAO;
import com.restaurant.dao.ReviewDAO;
import com.restaurant.model.ReviewListRow;
import com.restaurant.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ReviewService {

    private static final int RATING_MIN = 1;
    private static final int RATING_MAX = 5;

    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    public List<OrderDAO.PaidOrderSummary> listPaidOrdersForCustomer(long customerUserId) throws ServiceException {
        try {
            return orderDAO.listPaidOrdersForCustomer(customerUserId);
        } catch (SQLException e) {
            throw new ServiceException("Không tải được lịch sử order đã thanh toán.", e);
        }
    }

    public List<ReviewListRow> listAllReviewsForManager() throws ServiceException {
        try {
            return reviewDAO.listAllForManager();
        } catch (SQLException e) {
            throw new ServiceException("Không tải được danh sách đánh giá.", e);
        }
    }

    public void submitOrderLevelReview(long customerUserId, long orderId, int rating, String comment)
            throws ServiceException {
        validateRating(rating);
        String c = normalizeComment(comment);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (reviewDAO.findPaidOrderIdForCustomer(conn, customerUserId, orderId).isEmpty()) {
                    throw new ServiceException("Chỉ được đánh giá order đã thanh toán (PAID) và thuộc tài khoản của bạn.");
                }
                if (reviewDAO.existsOrderLevelReview(conn, customerUserId, orderId)) {
                    throw new ServiceException("Bạn đã gửi đánh giá tổng thể cho order này rồi.");
                }
                reviewDAO.insert(conn, customerUserId, orderId, null, rating, c);
                conn.commit();
            } catch (ServiceException e) {
                conn.rollback();
                throw e;
            } catch (SQLException e) {
                conn.rollback();
                throw duplicateOrDb("Không lưu được đánh giá.", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi kết nối CSDL.", e);
        }
    }

    public void submitDishReview(long customerUserId, long orderId, long menuItemId, int rating, String comment)
            throws ServiceException {
        validateRating(rating);
        String c = normalizeComment(comment);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (reviewDAO.findPaidOrderIdForCustomer(conn, customerUserId, orderId).isEmpty()) {
                    throw new ServiceException("Chỉ được đánh giá order đã thanh toán (PAID) và thuộc tài khoản của bạn.");
                }
                if (!reviewDAO.isMenuItemInPaidOrder(conn, customerUserId, orderId, menuItemId)) {
                    throw new ServiceException("Món này không có trong order đã thanh toán (hoặc dòng đã hủy).");
                }
                if (reviewDAO.existsDishReview(conn, customerUserId, orderId, menuItemId)) {
                    throw new ServiceException("Bạn đã đánh giá món này trong order đó rồi.");
                }
                reviewDAO.insert(conn, customerUserId, orderId, menuItemId, rating, c);
                conn.commit();
            } catch (ServiceException e) {
                conn.rollback();
                throw e;
            } catch (SQLException e) {
                conn.rollback();
                throw duplicateOrDb("Không lưu được đánh giá món.", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi kết nối CSDL.", e);
        }
    }

    public List<ReviewDAO.DishOption> listDishOptionsForPaidOrder(long customerUserId, long orderId)
            throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            if (reviewDAO.findPaidOrderIdForCustomer(conn, customerUserId, orderId).isEmpty()) {
                throw new ServiceException("Order không hợp lệ hoặc chưa thanh toán.");
            }
            return reviewDAO.listDishOptionsForPaidOrder(conn, customerUserId, orderId);
        } catch (SQLException e) {
            throw new ServiceException("Không đọc được danh sách món trong order.", e);
        }
    }

    private static void validateRating(int rating) throws ServiceException {
        if (rating < RATING_MIN || rating > RATING_MAX) {
            throw new ServiceException("Điểm đánh giá phải từ " + RATING_MIN + " đến " + RATING_MAX + " sao.");
        }
    }

    private static String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String t = comment.trim();
        return t.isEmpty() ? null : t;
    }

    private static ServiceException duplicateOrDb(String msg, SQLException e) {
        if (e.getErrorCode() == 1062 || (e.getMessage() != null && e.getMessage().contains("Duplicate"))) {
            return new ServiceException("Đánh giá trùng (có thể bạn đã gửi trước đó).", e);
        }
        return new ServiceException(msg, e);
    }
}
