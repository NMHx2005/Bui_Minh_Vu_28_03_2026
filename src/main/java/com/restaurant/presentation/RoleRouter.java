package com.restaurant.presentation;

import com.restaurant.model.User;

/**
 * Điều hướng theo vai trò sau đăng nhập (một điểm vào, dễ đọc).
 */
public final class RoleRouter {

    private RoleRouter() {
    }

    public static void openDashboard(ConsoleIO io, User user) {
        switch (user.getRole()) {
            case MANAGER -> new ManagerPresentation(io).run(user);
            case CHEF -> new ChefPresentation(io).run(user);
            case CUSTOMER -> new CustomerPresentation(io).run(user);
            default -> System.out.println("Vai trò không hỗ trợ.");
        }
    }
}
