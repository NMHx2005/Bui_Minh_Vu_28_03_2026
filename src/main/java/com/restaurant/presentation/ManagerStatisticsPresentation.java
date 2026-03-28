package com.restaurant.presentation;

import com.restaurant.service.ReportService;
import com.restaurant.service.ServiceException;
import com.restaurant.util.TablePrinter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Thống kê & báo cáo (10): doanh thu PAID + top món.
 */
public class ManagerStatisticsPresentation {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ConsoleIO io;
    private final ReportService reportService = new ReportService();

    public ManagerStatisticsPresentation(ConsoleIO io) {
        this.io = io;
    }

    public void run() {
        while (true) {
            System.out.println();
            System.out.println("========== Thống kê & báo cáo (0 = Quay lại menu Quản lý) ==========");
            System.out.println("Doanh thu lấy từ orders PAID theo ngày thanh toán (checked_out_at).");
            System.out.println("Top món: tổng quantity / doanh thu từ chi tiết (bỏ CANCELLED), cùng khoảng ngày.");
            System.out.println("1. Doanh thu theo khoảng ngày (tổng + chi tiết theo ngày)");
            System.out.println("2. Doanh thu theo tháng (nhập năm + tháng)");
            System.out.println("3. Top món bán chạy (theo số lượng)");
            System.out.println("4. Top món bán chạy (theo doanh thu dòng)");
            System.out.println("0. Quay lại");
            int c = io.readIntInRange("Chọn: ", 0, 4);
            if (c == 0) {
                return;
            }
            try {
                switch (c) {
                    case 1 -> revenueDateRange();
                    case 2 -> revenueMonth();
                    case 3 -> topByQuantity();
                    case 4 -> topByRevenue();
                    default -> {
                    }
                }
            } catch (ServiceException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private void revenueDateRange() throws ServiceException {
        LocalDate from = io.readLocalDateIso("Ngày bắt đầu");
        LocalDate to = io.readLocalDateIso("Ngày kết thúc");
        var total = reportService.totalRevenuePaidBetween(from, to);
        System.out.println("--- Tổng doanh thu (PAID) " + from.format(ISO) + " → " + to.format(ISO) + " ---");
        System.out.println(TablePrinter.formatMoney(total));
        var byDay = reportService.revenueByDayPaidBetween(from, to);
        TablePrinter.printRevenueByDayTable(byDay);
    }

    private void revenueMonth() throws ServiceException {
        int year = io.readIntInRange("Năm (ví dụ 2026): ", 2000, 2100);
        int month = io.readIntInRange("Tháng (1–12): ", 1, 12);
        YearMonth ym = YearMonth.of(year, month);
        LocalDate[] b = reportService.monthBounds(ym);
        var total = reportService.totalRevenuePaidBetween(b[0], b[1]);
        System.out.println("--- Doanh thu tháng " + month + "/" + year + " ---");
        System.out.println(TablePrinter.formatMoney(total));
        var byDay = reportService.revenueByDayPaidBetween(b[0], b[1]);
        TablePrinter.printRevenueByDayTable(byDay);
    }

    private void topByQuantity() throws ServiceException {
        LocalDate from = io.readLocalDateIso("Ngày bắt đầu");
        LocalDate to = io.readLocalDateIso("Ngày kết thúc");
        int n = io.readIntInRange("Top N món (1–50, mặc định nên dùng 10): ", 1, 50);
        var rows = reportService.topDishesByQuantity(from, to, n);
        TablePrinter.printTopDishesTable(rows, "Top " + n + " món (theo số lượng) " + from + " → " + to);
    }

    private void topByRevenue() throws ServiceException {
        LocalDate from = io.readLocalDateIso("Ngày bắt đầu");
        LocalDate to = io.readLocalDateIso("Ngày kết thúc");
        int n = io.readIntInRange("Top N món (1–50): ", 1, 50);
        var rows = reportService.topDishesByRevenue(from, to, n);
        TablePrinter.printTopDishesTable(rows, "Top " + n + " món (theo doanh thu) " + from + " → " + to);
    }
}
