package com.restaurant.service;

import com.restaurant.dao.UserDAO;
import com.restaurant.model.User;
import com.restaurant.util.PasswordHasher;

import java.sql.SQLException;

public class AuthService {

    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 100;
    private static final int PASSWORD_MIN = 6;
    private static final int PASSWORD_MAX = 128;

    private final UserDAO userDAO = new UserDAO();

    public User login(String username, String password) throws ServiceException {
        if (isBlank(username) || isBlank(password)) {
            throw new ServiceException("Tên đăng nhập và mật khẩu không được để trống.");
        }
        try {
            User u = userDAO.findByUsername(username.trim()).orElse(null);
            if (u == null || !PasswordHasher.verify(password, u.getPasswordHash())) {
                throw new ServiceException("Sai tên đăng nhập hoặc mật khẩu.");
            }
            if (!u.isActive()) {
                throw new ServiceException("Tài khoản đã bị vô hiệu hóa.");
            }
            return u;
        } catch (SQLException e) {
            throw new ServiceException("Lỗi cơ sở dữ liệu khi đăng nhập.", e);
        }
    }

    public void registerCustomer(String username, String password, String confirmPassword) throws ServiceException {
        validateUsername(username);
        validatePassword(password);
        if (!password.equals(confirmPassword)) {
            throw new ServiceException("Mật khẩu xác nhận không khớp.");
        }
        try {
            if (userDAO.existsByUsername(username.trim())) {
                throw new ServiceException("Tên đăng nhập đã tồn tại.");
            }
            String hash = PasswordHasher.hash(password);
            userDAO.insertCustomer(username.trim(), hash);
        } catch (SQLException e) {
            throw new ServiceException("Lỗi cơ sở dữ liệu khi đăng ký.", e);
        }
    }

    /** Dùng khi tạo tài khoản mới (đăng ký khách / tạo đầu bếp). */
    public void validateUsernameForNewAccount(String username) throws ServiceException {
        validateUsername(username);
    }

    public void validatePasswordForNewAccount(String password) throws ServiceException {
        validatePassword(password);
    }

    private void validateUsername(String username) throws ServiceException {
        if (username == null || username.isBlank()) {
            throw new ServiceException("Tên đăng nhập không được để trống.");
        }
        String u = username.trim();
        if (u.length() < USERNAME_MIN || u.length() > USERNAME_MAX) {
            throw new ServiceException("Tên đăng nhập phải từ " + USERNAME_MIN + " đến " + USERNAME_MAX + " ký tự.");
        }
        if (!u.matches("[a-zA-Z0-9_]+")) {
            throw new ServiceException("Tên đăng nhập chỉ gồm chữ, số và dấu gạch dưới.");
        }
    }

    private void validatePassword(String password) throws ServiceException {
        if (password == null || password.isBlank()) {
            throw new ServiceException("Mật khẩu không được để trống.");
        }
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw new ServiceException("Mật khẩu phải từ " + PASSWORD_MIN + " đến " + PASSWORD_MAX + " ký tự.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
