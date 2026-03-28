package com.restaurant.presentation;

import com.restaurant.service.ReviewService;
import com.restaurant.service.ServiceException;
import com.restaurant.util.TablePrinter;

/** Xem tổng hợp đánh giá (09). */
public class ManagerReviewPresentation {

    private final ConsoleIO io;
    private final ReviewService reviewService = new ReviewService();

    public ManagerReviewPresentation(ConsoleIO io) {
        this.io = io;
    }

    public void run() {
        while (true) {
            System.out.println();
            System.out.println("----- Đánh giá từ khách (0 = Quay lại menu Quản lý) -----");
            System.out.println("1. Xem tất cả đánh giá (mới nhất trước)");
            System.out.println("0. Quay lại");
            int c = io.readIntInRange("Chọn: ", 0, 1);
            if (c == 0) {
                return;
            }
            try {
                var list = reviewService.listAllReviewsForManager();
                TablePrinter.printReviewsTable(list);
            } catch (ServiceException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
}
