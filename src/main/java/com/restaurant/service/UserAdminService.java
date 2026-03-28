package com.restaurant.service;

import com.restaurant.dao.UserDAO;
import com.restaurant.model.Role;
import com.restaurant.model.User;
import com.restaurant.util.PasswordHasher;

import java.sql.SQLException;
import java.util.List;

/**
 * Quản lý người dùng (manager): tạo đầu bếp, vô hiệu hóa tài khoản với rule an toàn.
 */
public class UserAdminService {

    private final UserDAO userDAO = new UserDAO();
    private final AuthService authService = new AuthService();

    public List<User> listUsers() throws ServiceException {
        try {
            return userDAO.listAllForAdmin();
        } catch (SQLException e) {
            throw new ServiceException("Không tải được danh sách người dùng.", e);
        }
    }

    public long createChef(String username, String password, String confirmPassword) throws ServiceException {
        authService.validateUsernameForNewAccount(username);
        authService.validatePasswordForNewAccount(password);
        if (!password.equals(confirmPassword)) {
            throw new ServiceException("Mật khẩu xác nhận không khớp.");
        }
        try {
            String u = username.trim();
            if (userDAO.existsByUsername(u)) {
                throw new ServiceException("Tên đăng nhập đã tồn tại.");
            }
            String hash = PasswordHasher.hash(password);
            return userDAO.insertChef(u, hash);
        } catch (SQLException e) {
            throw new ServiceException("Lỗi CSDL khi tạo tài khoản đầu bếp.", e);
        }
    }

    /**
     * Vô hiệu hóa user theo id.
     *
     * @param actingManagerId manager đang thao tác (không cho tự vô hiệu chính mình)
     */
    public void deactivateUser(long actingManagerId, long targetUserId) throws ServiceException {
        if (targetUserId == actingManagerId) {
            throw new ServiceException("Không thể vô hiệu hóa chính tài khoản đang đăng nhập.");
        }
        try {
            User target = userDAO.findById(targetUserId)
                    .orElseThrow(() -> new ServiceException("Không tìm thấy người dùng có id = " + targetUserId + "."));
            if (!target.isActive()) {
                throw new ServiceException("Tài khoản này đã bị vô hiệu hóa trước đó.");
            }
            if (target.getRole() == Role.MANAGER) {
                int activeManagers = userDAO.countActiveByRole(Role.MANAGER);
                if (activeManagers <= 1) {
                    throw new ServiceException("Không thể vô hiệu hóa manager cuối cùng đang hoạt động.");
                }
            }
            int n = userDAO.setActive(targetUserId, false);
            if (n != 1) {
                throw new ServiceException("Không cập nhật được trạng thái tài khoản.");
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi CSDL khi vô hiệu hóa.", e);
        }
    }
}
