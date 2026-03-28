package com.restaurant.presentation;

import com.restaurant.model.User;

public class ChefPresentation {

    private final ConsoleIO io;

    public ChefPresentation(ConsoleIO io) {
        this.io = io;
    }

    public void run(User chef) {
        while (true) {
            System.out.println();
            System.out.println("========== Đầu bếp — " + chef.getUsername() + " ==========");
            System.out.println("Chức năng bếp (xem món, cập nhật trạng thái) triển khai ở buổi 4.");
            System.out.println("0. Đăng xuất");
            int c = io.readIntInRange("Chọn: ", 0, 0);
            if (c == 0) {
                return;
            }
        }
    }
}
