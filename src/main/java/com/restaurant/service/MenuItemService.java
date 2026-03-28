package com.restaurant.service;

import com.restaurant.dao.MenuItemDAO;
import com.restaurant.model.MenuItem;
import com.restaurant.model.MenuItemType;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class MenuItemService {

    public static final int NAME_MIN_LEN = 2;
    public static final int NAME_MAX_LEN = 200;
    private static final BigDecimal PRICE_MAX = new BigDecimal("999999999.99");
    private static final int STOCK_MAX = 10_000_000;

    private final MenuItemDAO menuItemDAO = new MenuItemDAO();

    public List<MenuItem> listAll() throws ServiceException {
        try {
            return menuItemDAO.findAll();
        } catch (SQLException e) {
            throw new ServiceException("Không đọc được thực đơn.", e);
        }
    }

    public List<MenuItem> search(String keyword) throws ServiceException {
        if (keyword == null || keyword.isBlank()) {
            throw new ServiceException("Từ khóa tìm kiếm không được để trống.");
        }
        try {
            return menuItemDAO.searchByNameContains(keyword.trim());
        } catch (SQLException e) {
            throw new ServiceException("Lỗi khi tìm kiếm món.", e);
        }
    }

    public MenuItem getById(long id) throws ServiceException {
        try {
            var opt = menuItemDAO.findById(id);
            if (opt.isEmpty()) {
                throw new ServiceException("Không tìm thấy món có id = " + id + ".");
            }
            return opt.get();
        } catch (SQLException e) {
            throw new ServiceException("Lỗi khi đọc món.", e);
        }
    }

    public long add(String name, MenuItemType type, BigDecimal price, Integer stockQuantity) throws ServiceException {
        validateName(name);
        validatePrice(price);
        Integer stock = normalizeStock(type, stockQuantity);
        try {
            return menuItemDAO.insert(name.trim(), type, price, stock);
        } catch (SQLException e) {
            throw new ServiceException("Không thêm được món.", e);
        }
    }

    public void update(long id, String name, MenuItemType type, BigDecimal price, Integer stockQuantity) throws ServiceException {
        validateName(name);
        validatePrice(price);
        Integer stock = normalizeStock(type, stockQuantity);
        try {
            if (menuItemDAO.findById(id).isEmpty()) {
                throw new ServiceException("Không tìm thấy món có id = " + id + ".");
            }
            if (!menuItemDAO.update(id, name.trim(), type, price, stock)) {
                throw new ServiceException("Cập nhật món thất bại.");
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi khi cập nhật món.", e);
        }
    }

    public void softDelete(long id) throws ServiceException {
        try {
            if (menuItemDAO.findById(id).isEmpty()) {
                throw new ServiceException("Không tìm thấy món có id = " + id + ".");
            }
            if (!menuItemDAO.softDelete(id)) {
                throw new ServiceException("Xóa món thất bại.");
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi khi xóa món.", e);
        }
    }

    private void validateName(String name) throws ServiceException {
        if (name == null || name.isBlank()) {
            throw new ServiceException("Tên món không được để trống.");
        }
        String t = name.trim();
        if (t.length() < NAME_MIN_LEN || t.length() > NAME_MAX_LEN) {
            throw new ServiceException("Tên món phải từ " + NAME_MIN_LEN + " đến " + NAME_MAX_LEN + " ký tự.");
        }
    }

    private void validatePrice(BigDecimal price) throws ServiceException {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("Giá phải là số dương.");
        }
        if (price.compareTo(PRICE_MAX) > 0) {
            throw new ServiceException("Giá vượt quá giới hạn cho phép.");
        }
    }

    private Integer normalizeStock(MenuItemType type, Integer stockQuantity) throws ServiceException {
        if (type == MenuItemType.FOOD) {
            return null;
        }
        if (stockQuantity == null) {
            throw new ServiceException("Đồ uống cần nhập số lượng tồn kho (>= 0).");
        }
        if (stockQuantity < 0 || stockQuantity > STOCK_MAX) {
            throw new ServiceException("Tồn kho không hợp lệ (0 – " + STOCK_MAX + ").");
        }
        return stockQuantity;
    }
}
