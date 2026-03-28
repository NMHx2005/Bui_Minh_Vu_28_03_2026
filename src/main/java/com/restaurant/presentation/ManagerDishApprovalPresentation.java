package com.restaurant.presentation;

import com.restaurant.dao.OrderDAO;
import com.restaurant.model.OrderLineView;
import com.restaurant.service.ManagerDishApprovalService;
import com.restaurant.service.ServiceException;
import com.restaurant.util.TablePrinter;

import java.util.List;

/** Duyệt / từ chối món theo order (nâng cao 08). */
public class ManagerDishApprovalPresentation {

    private final ConsoleIO io;
    private final ManagerDishApprovalService approvalService = new ManagerDishApprovalService();

    public ManagerDishApprovalPresentation(ConsoleIO io) {
        this.io = io;
    }

    public void run() {
        while (true) {
            System.out.println();
            System.out.println("----- Duyệt món theo bàn / order (0 = Quay lại menu Quản lý) -----");
            System.out.println("Quy ước: đồ ăn — quản lý duyệt sau READY; đồ uống — duyệt trước (trừ kho khi duyệt).");
            System.out.println("1. Xem order OPEN + chọn order");
            System.out.println("2. Duyệt một dòng (nhập ID dòng)");
            System.out.println("3. Từ chối một dòng (nhập ID dòng)");
            System.out.println("4. Duyệt hàng loạt mọi dòng đủ điều kiện trong order");
            System.out.println("0. Quay lại");
            int c = io.readIntInRange("Chọn: ", 0, 4);
            try {
                switch (c) {
                    case 0 -> {
                        return;
                    }
                    case 1 -> pickOrderAndShowLines();
                    case 2 -> approveOne();
                    case 3 -> rejectOne();
                    case 4 -> batchApprove();
                    default -> {
                    }
                }
            } catch (ServiceException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private long readOrderId() throws ServiceException {
        List<OrderDAO.OpenOrderSummary> open = approvalService.listOpenOrders();
        if (open.isEmpty()) {
            throw new ServiceException("Hiện không có order OPEN nào.");
        }
        System.out.println("--- Order đang mở ---");
        String line = "---------------------------------------------";
        System.out.println(line);
        System.out.printf("%-10s %-10s %s%n", "Order #", "Bàn (id)", "Mã bàn");
        System.out.println(line);
        for (var s : open) {
            System.out.printf("%-10d %-10d %s%n", s.orderId(), s.tableId(), s.tableCode());
        }
        System.out.println(line);
        return io.readLong("Nhập mã order: ");
    }

    private void pickOrderAndShowLines() throws ServiceException {
        long orderId = readOrderId();
        List<OrderLineView> lines = approvalService.listLinesForOrder(orderId);
        if (lines.isEmpty()) {
            System.out.println("Order không có dòng chi tiết.");
            return;
        }
        TablePrinter.printOrderLinesTable(lines);
    }

    private void approveOne() throws ServiceException {
        long orderId = readOrderId();
        List<OrderLineView> lines = approvalService.listLinesForOrder(orderId);
        TablePrinter.printOrderLinesTable(lines);
        long detailId = io.readLong("ID dòng cần duyệt: ");
        approvalService.approveLine(detailId);
        System.out.println("Đã duyệt dòng #" + detailId + ".");
    }

    private void rejectOne() throws ServiceException {
        long orderId = readOrderId();
        List<OrderLineView> lines = approvalService.listLinesForOrder(orderId);
        TablePrinter.printOrderLinesTable(lines);
        long detailId = io.readLong("ID dòng cần từ chối: ");
        if (!io.readYesNo("Xác nhận từ chối dòng #" + detailId + "? (Y/N): ")) {
            System.out.println("Đã hủy.");
            return;
        }
        approvalService.rejectLine(detailId);
        System.out.println("Đã từ chối (dòng chuyển CANCELLED, không tính tiền).");
    }

    private void batchApprove() throws ServiceException {
        long orderId = readOrderId();
        List<OrderLineView> lines = approvalService.listLinesForOrder(orderId);
        TablePrinter.printOrderLinesTable(lines);
        if (!io.readYesNo("Thử duyệt tất cả dòng đang Chờ duyệt QL trong order #" + orderId + "? (Y/N): ")) {
            System.out.println("Đã hủy.");
            return;
        }
        List<String> errs = approvalService.batchApproveOrder(orderId);
        for (String e : errs) {
            System.out.println(e);
        }
    }
}
