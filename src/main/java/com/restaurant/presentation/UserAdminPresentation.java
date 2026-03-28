package com.restaurant.presentation;

import com.restaurant.model.User;
import com.restaurant.service.ServiceException;
import com.restaurant.service.UserAdminService;
import com.restaurant.util.TablePrinter;

public class UserAdminPresentation {

    private final ConsoleIO io;
    private final UserAdminService userAdminService = new UserAdminService();

    public UserAdminPresentation(ConsoleIO io) {
        this.io = io;
    }

    public void run(User manager) {
        while (true) {
            System.out.println();
            System.out.println("----- Quản lý người dùng (0 = Quay lại menu Quản lý) -----");
            System.out.println("1. Liệt kê người dùng");
            System.out.println("2. Tạo tài khoản đầu bếp (CHEF)");
            System.out.println("3. Vô hiệu hóa tài khoản");
            System.out.println("0. Quay lại");
            int c = io.readIntInRange("Chọn: ", 0, 3);
            try {
                switch (c) {
                    case 0 -> {
                        return;
                    }
                    case 1 -> listUsers();
                    case 2 -> createChef();
                    case 3 -> deactivateUser(manager.getId());
                    default -> {
                    }
                }
            } catch (ServiceException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private void listUsers() throws ServiceException {
        var list = userAdminService.listUsers();
        TablePrinter.printUsersAdminTable(list);
    }

    private void createChef() throws ServiceException {
        System.out.println("Tạo tài khoản đầu bếp (quy tắc tên/mật khẩu giống đăng ký khách).");
        String username = io.readNonBlankLine("Tên đăng nhập (3–100 ký tự, chữ/số/_): ");
        String password = io.readNonBlankLine("Mật khẩu (6–128 ký tự): ");
        String confirm = io.readNonBlankLine("Nhập lại mật khẩu: ");
        long id = userAdminService.createChef(username, password, confirm);
        System.out.println("Đã tạo đầu bếp. Id user = " + id);
    }

    private void deactivateUser(long actingManagerId) throws ServiceException {
        var list = userAdminService.listUsers();
        TablePrinter.printUsersAdminTable(list);
        long targetId = io.readLong("Nhập id người dùng cần vô hiệu hóa: ");
        if (!io.readYesNo("Xác nhận vô hiệu hóa user id " + targetId + "? (Y/N): ")) {
            System.out.println("Đã hủy.");
            return;
        }
        userAdminService.deactivateUser(actingManagerId, targetId);
        System.out.println("Đã vô hiệu hóa tài khoản.");
    }
}
