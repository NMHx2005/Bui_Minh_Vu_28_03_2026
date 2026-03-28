package com.restaurant.presentation;

import com.restaurant.model.MenuItem;
import com.restaurant.model.MenuItemType;
import com.restaurant.model.User;
import com.restaurant.service.CheckoutService;
import com.restaurant.service.DiningTableService;
import com.restaurant.service.MenuItemService;
import com.restaurant.service.ServiceException;
import com.restaurant.util.TablePrinter;

import java.util.List;

public class ManagerPresentation {

    private final ConsoleIO io;
    private final MenuItemService menuItemService = new MenuItemService();
    private final DiningTableService diningTableService = new DiningTableService();
    private final CheckoutService checkoutService = new CheckoutService();

    public ManagerPresentation(ConsoleIO io) {
        this.io = io;
    }

    public void run(User manager) {
        while (true) {
            System.out.println();
            System.out.println("========== Quản lý — Hệ thống Quản lý Nhà hàng (0 = Đăng xuất) ==========");
            System.out.println("Xin chào, " + manager.getUsername());
            System.out.println("1. Quản lý thực đơn (món ăn / đồ uống)");
            System.out.println("2. Quản lý bàn ăn");
            System.out.println("3. Thanh toán & hóa đơn (order OPEN — nhập mã order)");
            System.out.println("0. Đăng xuất");
            int c = io.readIntInRange("Chọn: ", 0, 3);
            if (c == 0) {
                return;
            }
            if (c == 1) {
                menuLoop();
            } else if (c == 2) {
                tableLoop();
            } else {
                try {
                    managerCheckout();
                } catch (ServiceException e) {
                    System.out.println("Lỗi: " + e.getMessage());
                }
            }
        }
    }

    private void menuLoop() {
        while (true) {
            System.out.println();
            System.out.println("----- Quản lý thực đơn (0 = Quay lại menu Quản lý) -----");
            System.out.println("1. Hiển thị tất cả món");
            System.out.println("2. Thêm món");
            System.out.println("3. Sửa món");
            System.out.println("4. Xóa món (ẩn khỏi thực đơn)");
            System.out.println("5. Tìm món theo tên");
            System.out.println("0. Quay lại");
            int c = io.readIntInRange("Chọn: ", 0, 5);
            try {
                switch (c) {
                    case 0 -> {
                        return;
                    }
                    case 1 -> listAllItems();
                    case 2 -> addItem();
                    case 3 -> updateItem();
                    case 4 -> deleteItem();
                    case 5 -> searchItems();
                    default -> {
                    }
                }
            } catch (ServiceException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private void listAllItems() throws ServiceException {
        List<MenuItem> list = menuItemService.listAll();
        TablePrinter.printMenuItemsTable(list);
    }

    private void addItem() throws ServiceException {
        String name = io.readNonBlankLine("Tên món: ");
        MenuItemType type = readItemType();
        var price = io.readPositiveBigDecimal("Giá (VNĐ): ");
        Integer stock = null;
        if (type == MenuItemType.DRINK) {
            stock = io.readNonNegativeInt("Số lượng tồn kho: ");
        }
        long id = menuItemService.add(name, type, price, stock);
        System.out.println("Thêm món thành công. Id món = " + id);
    }

    private void updateItem() throws ServiceException {
        long id = io.readLong("Id món cần sửa: ");
        MenuItem cur = menuItemService.getById(id);
        System.out.println("Thông tin hiện tại:");
        TablePrinter.printMenuItemsTable(List.of(cur));
        String name = io.readNonBlankLine("Tên món mới: ");
        MenuItemType type = readItemType();
        var price = io.readPositiveBigDecimal("Giá mới (VNĐ): ");
        Integer stock = null;
        if (type == MenuItemType.DRINK) {
            stock = io.readNonNegativeInt("Tồn kho mới: ");
        }
        menuItemService.update(id, name, type, price, stock);
        System.out.println("Sửa món thành công.");
    }

    private void deleteItem() throws ServiceException {
        long id = io.readLong("Id món cần xóa: ");
        menuItemService.getById(id);
        if (!io.readYesNo("Bạn có chắc muốn ẩn món này khỏi thực đơn? (Y/N): ")) {
            System.out.println("Đã hủy.");
            return;
        }
        menuItemService.softDelete(id);
        System.out.println("Đã ẩn món (soft delete).");
    }

    private void searchItems() throws ServiceException {
        String kw = io.readNonBlankLine("Nhập tên (tìm tương đối): ");
        List<MenuItem> list = menuItemService.search(kw);
        if (list.isEmpty()) {
            System.out.println("Không tìm thấy món nào.");
        } else {
            TablePrinter.printMenuItemsTable(list);
        }
    }

    private MenuItemType readItemType() {
        while (true) {
            System.out.println("Loại: 1 = Đồ ăn, 2 = Đồ uống");
            int t = io.readIntInRange("Chọn loại: ", 1, 2);
            if (t == 1) {
                return MenuItemType.FOOD;
            }
            return MenuItemType.DRINK;
        }
    }

    private void tableLoop() {
        while (true) {
            System.out.println();
            System.out.println("----- Quản lý bàn ăn (0 = Quay lại menu Quản lý) -----");
            System.out.println("1. Hiển thị tất cả bàn");
            System.out.println("2. Thêm bàn");
            System.out.println("3. Sửa bàn");
            System.out.println("4. Xóa bàn");
            System.out.println("5. Tìm bàn theo mã");
            System.out.println("0. Quay lại");
            int c = io.readIntInRange("Chọn: ", 0, 5);
            try {
                switch (c) {
                    case 0 -> {
                        return;
                    }
                    case 1 -> listAllTables();
                    case 2 -> addTable();
                    case 3 -> updateTable();
                    case 4 -> deleteTable();
                    case 5 -> searchTables();
                    default -> {
                    }
                }
            } catch (ServiceException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private void listAllTables() throws ServiceException {
        TablePrinter.printDiningTablesTable(diningTableService.listAll());
    }

    private void addTable() throws ServiceException {
        String code = io.readNonBlankLine("Mã bàn: ");
        int cap = io.readIntInRange("Sức chứa (số chỗ, 1–500): ", 1, 500);
        long id = diningTableService.add(code, cap);
        System.out.println("Thêm bàn thành công. Id bàn = " + id);
    }

    private void updateTable() throws ServiceException {
        long id = io.readLong("Id bàn cần sửa: ");
        var cur = diningTableService.getById(id);
        System.out.println("Hiện tại: mã " + cur.getTableCode() + ", sức chứa " + cur.getCapacity());
        String code = io.readNonBlankLine("Mã bàn mới: ");
        int cap = io.readIntInRange("Sức chứa mới (1–500): ", 1, 500);
        diningTableService.update(id, code, cap);
        System.out.println("Sửa bàn thành công.");
    }

    private void deleteTable() throws ServiceException {
        long id = io.readLong("Id bàn cần xóa: ");
        diningTableService.getById(id);
        if (!io.readYesNo("Xóa hẳn bàn này khỏi CSDL? (Y/N): ")) {
            System.out.println("Đã hủy.");
            return;
        }
        diningTableService.delete(id);
        System.out.println("Xóa bàn thành công.");
    }

    private void searchTables() throws ServiceException {
        String kw = io.readNonBlankLine("Nhập mã bàn (tìm tương đối): ");
        var list = diningTableService.searchByCode(kw);
        if (list.isEmpty()) {
            System.out.println("Không tìm thấy bàn nào.");
        } else {
            TablePrinter.printDiningTablesTable(list);
        }
    }

    private void managerCheckout() throws ServiceException {
        long orderId = io.readLong("Mã order OPEN cần thanh toán: ");
        var preview = checkoutService.loadOpenOrderPreview(orderId);
        System.out.println("--- Chi tiết order #" + orderId + " ---");
        TablePrinter.printOrderLinesTable(preview.lines());
        System.out.printf("Tạm tính (bỏ qua món đã hủy): %s%n",
                TablePrinter.formatMoney(TablePrinter.sumBillableSubtotal(preview.lines())));
        System.out.println("Quy ước: chỉ thanh toán khi mọi món (trừ đã hủy) đã SERVED.");
        if (!io.readYesNo("Xác nhận thanh toán? (Y/N): ")) {
            System.out.println("Đã hủy.");
            return;
        }
        var inv = checkoutService.checkoutAsManager(orderId);
        TablePrinter.printCheckoutInvoice(inv);
        System.out.println("Đã thanh toán và giải phóng bàn.");
    }
}
