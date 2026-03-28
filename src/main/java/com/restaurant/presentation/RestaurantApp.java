package com.restaurant.presentation;

import com.restaurant.model.User;
import com.restaurant.service.AuthService;
import com.restaurant.service.ServiceException;

import java.util.Scanner;

public class RestaurantApp {

    private final ConsoleIO io;
    private final AuthService authService = new AuthService();

    public RestaurantApp(Scanner scanner) {
        this.io = new ConsoleIO(scanner);
    }

    public void run() {
        try {
            runInternal();
        } catch (Exception e) {
            System.out.println();
            System.out.println("Đã xảy ra lỗi hệ thống. Vui lòng kiểm tra kết nối CSDL và thử lại.");
        }
    }

    private void runInternal() {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   HỆ THỐNG QUẢN LÝ NHÀ HÀNG (Console)  ║");
        System.out.println("╚════════════════════════════════════════╝");
        while (true) {
            System.out.println();
            System.out.println("---------- Menu chính (0 = thoát) ----------");
            System.out.println("1. Đăng nhập");
            System.out.println("2. Đăng ký tài khoản khách hàng");
            System.out.println("0. Thoát chương trình");
            int choice = io.readIntInRange("Chọn: ", 0, 2);
            if (choice == 0) {
                System.out.println("Tạm biệt.");
                return;
            }
            if (choice == 2) {
                registerCustomer();
                continue;
            }
            User user = loginLoop();
            if (user == null) {
                continue;
            }
            RoleRouter.openDashboard(io, user);
        }
    }

    /**
     * @return user đăng nhập thành công; {@code null} nếu người dùng quay lại menu chính.
     */
    private User loginLoop() {
        while (true) {
            System.out.println();
            System.out.println("---------- Đăng nhập ----------");
            System.out.println("(Nhập 0 làm tên đăng nhập để quay lại)");
            String username = io.readLine("Tên đăng nhập: ").trim();
            if ("0".equals(username)) {
                return null;
            }
            String password = io.readLine("Mật khẩu: ");
            try {
                User u = authService.login(username, password);
                System.out.println("Đăng nhập thành công.");
                return u;
            } catch (ServiceException e) {
                System.out.println(e.getMessage());
                System.out.println("Vui lòng thử lại.");
            }
        }
    }

    private void registerCustomer() {
        System.out.println();
        System.out.println("---------- Đăng ký khách hàng ----------");
        try {
            String username = io.readNonBlankLine("Tên đăng nhập (3–100 ký tự, chữ/số/_): ");
            String password = io.readNonBlankLine("Mật khẩu (6–128 ký tự): ");
            String confirm = io.readNonBlankLine("Nhập lại mật khẩu: ");
            authService.registerCustomer(username, password, confirm);
            System.out.println("Đăng ký thành công. Bạn có thể đăng nhập với tài khoản vừa tạo.");
        } catch (ServiceException e) {
            System.out.println("Lỗi: " + e.getMessage());
        }
    }

}
