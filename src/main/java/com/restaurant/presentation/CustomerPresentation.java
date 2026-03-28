package com.restaurant.presentation;

import com.restaurant.model.DiningTable;
import com.restaurant.model.User;
import com.restaurant.service.CheckoutService;
import com.restaurant.service.CustomerOrderService;
import com.restaurant.service.ServiceException;
import com.restaurant.util.TablePrinter;

public class CustomerPresentation {

    private final ConsoleIO io;
    private final CustomerOrderService orderService = new CustomerOrderService();
    private final CheckoutService checkoutService = new CheckoutService();

    public CustomerPresentation(ConsoleIO io) {
        this.io = io;
    }

    public void run(User customer) {
        long customerId = customer.getId();
        while (true) {
            System.out.println();
            System.out.println("========== Khách hàng — " + customer.getUsername() + " (0 = Đăng xuất) ==========");
            try {
                printSessionLine(customerId);
            } catch (ServiceException e) {
                System.out.println("(Không tải trạng thái phiên: " + e.getMessage() + ")");
            }
            System.out.println("1. Xem thực đơn (đang phục vụ)");
            System.out.println("2. Chọn bàn trống — bắt đầu phiên gọi món");
            System.out.println("3. Gọi món (cần đã chọn bàn)");
            System.out.println("4. Theo dõi món đã gọi");
            System.out.println("5. Hủy món (PENDING bếp + chưa duyệt quản lý)");
            System.out.println("6. Thanh toán — in hóa đơn (phiên OPEN của bạn)");
            System.out.println("0. Đăng xuất");
            int choice = io.readIntInRange("Chọn: ", 0, 6);
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
                    case 6 -> checkoutCustomer(customerId);
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
        System.out.println("Danh sách bàn đang trống (chỉ nhập ID có trong bảng):");
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

    private void checkoutCustomer(long customerId) throws ServiceException {
        var lines = orderService.listMyOrderLines(customerId);
        if (lines.isEmpty()) {
            System.out.println("Bạn chưa có order hoặc order chưa có món — không thể thanh toán.");
            return;
        }
        System.out.println("--- Chi tiết order (trước khi thanh toán) ---");
        TablePrinter.printOrderLinesTable(lines);
        System.out.printf("Tạm tính (bỏ qua món đã hủy): %s%n",
                TablePrinter.formatMoney(TablePrinter.sumBillableSubtotal(lines)));
        System.out.println("Quy ước: chỉ thanh toán khi mọi món (trừ đã hủy) đã SERVED.");
        if (!io.readYesNo("Xác nhận thanh toán? (Y/N): ")) {
            System.out.println("Đã hủy.");
            return;
        }
        var inv = checkoutService.checkoutForCustomer(customerId);
        TablePrinter.printCheckoutInvoice(inv);
        System.out.println("Đã thanh toán. Bàn được giải phóng — có thể chọn bàn mới (mục 2).");
    }
}
