package com.restaurant.presentation;

import com.restaurant.model.User;

public class CustomerPresentation {

    private final ConsoleIO io;

    public CustomerPresentation(ConsoleIO io) {
        this.io = io;
    }

    public void run(User customer) {
        while (true) {
            System.out.println();
            System.out.println("========== Khách hàng — " + customer.getUsername() + " ==========");
            System.out.println("Xem menu, chọn bàn, gọi món… triển khai ở buổi 3.");
            System.out.println("0. Đăng xuất");
            int c = io.readIntInRange("Chọn: ", 0, 0);
            if (c == 0) {
                return;
            }
        }
    }
}
