package com.restaurant.presentation;

import com.restaurant.model.DiningTable;
import com.restaurant.model.User;
import com.restaurant.service.CustomerOrderService;
import com.restaurant.service.ServiceException;
import com.restaurant.util.TablePrinter;

public class CustomerPresentation {

    private final ConsoleIO io;
    private final CustomerOrderService orderService = new CustomerOrderService();

    public CustomerPresentation(ConsoleIO io) {
        this.io = io;
    }

    public void run(User customer) {
        long customerId = customer.getId();
        while (true) {
            System.out.println();
            System.out.println("========== Khách hàng — " + customer.getUsername() + " ==========");
            try {
                printSessionLine(customerId);
            } catch (ServiceException e) {
                System.out.println("(Không tải trạng thái phiên: " + e.getMessage() + ")");
            }
            System.out.println("1. Xem thực đơn (đang phục vụ)");
            System.out.println("2. Chọn bàn trống — bắt đầu phiên gọi món");
            System.out.println("3. Gọi món (cần đã chọn bàn)");
            System.out.println("4. Theo dõi món đã gọi");
            System.out.println("5. Hủy món (chỉ khi trạng thái Chờ / PENDING)");
            System.out.println("0. Đăng xuất");
            int choice = io.readIntInRange("Chọn: ", 0, 5);
            if (choice == 0) {
                return;
            }
            try {
                switch (choice) {
                    case 1 -> viewMenu();
                    case 2 -> chooseTable(customerId);
                    case 3 -> placeOrder(customerId);
                    case 4 -> trackOrder(customerId);
                    case 5 -> cancelLine(customerId);
                    default -> {
                    }
                }
            } catch (ServiceException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private void printSessionLine(long customerId) throws ServiceException {
        var orderOpt = orderService.getOpenOrder(customerId);
        if (orderOpt.isEmpty()) {
            System.out.println("→ Chưa có phiên: chọn bàn trống (mục 2) trước khi gọi món.");
            return;
        }
        var order = orderOpt.get();
        var tableOpt = orderService.getTable(order.getTableId());
        String code = tableOpt.map(DiningTable::getTableCode).orElse("?");
        System.out.println("→ Phiên hiện tại: Order #" + order.getId() + " | Bàn " + code);
    }

    private void viewMenu() throws ServiceException {
        var items = orderService.listActiveMenu();
        TablePrinter.printCustomerMenu(items);
    }

    private void chooseTable(long customerId) throws ServiceException {
        var free = orderService.listFreeTables();
        if (free.isEmpty()) {
            System.out.println("Hiện không có bàn trống.");
            return;
        }
        System.out.println("Danh sách bàn đang trống:");
        TablePrinter.printDiningTablesTable(free);
        long tableId = io.readLong("Nhập ID bàn muốn ngồi: ");
        long orderId = orderService.startSessionAtTable(customerId, tableId);
        System.out.println("Đã chọn bàn và tạo order. Mã phiên: #" + orderId);
    }

    private void placeOrder(long customerId) throws ServiceException {
        var menu = orderService.listActiveMenu();
        if (menu.isEmpty()) {
            System.out.println("Không có món để gọi.");
            return;
        }
        TablePrinter.printCustomerMenu(menu);
        long itemId = io.readLong("ID món cần gọi: ");
        int qty = io.readIntInRange("Số lượng (1–999): ", 1, 999);
        orderService.addOrderLine(customerId, itemId, qty);
        System.out.println("Đã thêm món vào order.");
    }

    private void trackOrder(long customerId) throws ServiceException {
        var lines = orderService.listMyOrderLines(customerId);
        if (lines.isEmpty()) {
            System.out.println("Bạn chưa có order hoặc order chưa có món.");
            return;
        }
        TablePrinter.printOrderLinesTable(lines);
    }

    private void cancelLine(long customerId) throws ServiceException {
        var lines = orderService.listMyOrderLines(customerId);
        if (lines.isEmpty()) {
            System.out.println("Không có dòng nào để hủy.");
            return;
        }
        TablePrinter.printOrderLinesTable(lines);
        long detailId = io.readLong("Nhập ID dòng (cột ID dòng) muốn hủy: ");
        orderService.cancelOrderLine(customerId, detailId);
        System.out.println("Đã hủy dòng order (trạng thái CANCELLED).");
    }
}
