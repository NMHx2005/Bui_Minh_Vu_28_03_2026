package com.restaurant.presentation;

import com.restaurant.model.User;
import com.restaurant.service.ChefService;
import com.restaurant.service.ServiceException;
import com.restaurant.util.TablePrinter;

public class ChefPresentation {

    private final ConsoleIO io;
    private final ChefService chefService = new ChefService();

    public ChefPresentation(ConsoleIO io) {
        this.io = io;
    }

    public void run(User chef) {
        while (true) {
            System.out.println();
            System.out.println("========== Đầu bếp — " + chef.getUsername() + " ==========");
            System.out.println("1. Xem hàng đợi (PENDING / Đang nấu / Sẵn sàng)");
            System.out.println("2. Cập nhật trạng thái món (một bước: Chờ→Nấu→Sẵn sàng→Phục vụ)");
            System.out.println("0. Đăng xuất");
            int choice = io.readIntInRange("Chọn: ", 0, 2);
            if (choice == 0) {
                return;
            }
            try {
                if (choice == 1) {
                    showQueue();
                } else {
                    advanceOneStep();
                }
            } catch (ServiceException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private void showQueue() throws ServiceException {
        var lines = chefService.listKitchenQueue();
        TablePrinter.printChefKitchenQueue(lines);
    }

    private void advanceOneStep() throws ServiceException {
        showQueue();
        long id = io.readLong("Nhập ID dòng (cột ID dòng) cần chuyển bước tiếp: ");
        chefService.advanceLineStatus(id);
        System.out.println("Đã cập nhật trạng thái thành công.");
    }
}
