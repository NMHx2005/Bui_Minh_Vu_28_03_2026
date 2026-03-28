package com.restaurant.service;

import com.restaurant.dao.DiningTableDAO;
import com.restaurant.model.DiningTable;
import com.restaurant.model.TableStatus;

import java.sql.SQLException;
import java.util.List;

public class DiningTableService {

    public static final int CODE_MIN_LEN = 1;
    public static final int CODE_MAX_LEN = 50;
    private static final int CAPACITY_MIN = 1;
    private static final int CAPACITY_MAX = 500;

    private final DiningTableDAO diningTableDAO = new DiningTableDAO();

    public List<DiningTable> listAll() throws ServiceException {
        try {
            return diningTableDAO.findAll();
        } catch (SQLException e) {
            throw new ServiceException("Không đọc được danh sách bàn.", e);
        }
    }

    public List<DiningTable> searchByCode(String keyword) throws ServiceException {
        if (keyword == null || keyword.isBlank()) {
            throw new ServiceException("Mã bàn tìm kiếm không được để trống.");
        }
        try {
            return diningTableDAO.searchByCodeContains(keyword.trim());
        } catch (SQLException e) {
            throw new ServiceException("Lỗi khi tìm bàn.", e);
        }
    }

    public DiningTable getById(long id) throws ServiceException {
        try {
            var opt = diningTableDAO.findById(id);
            if (opt.isEmpty()) {
                throw new ServiceException("Không tìm thấy bàn có id = " + id + ".");
            }
            return opt.get();
        } catch (SQLException e) {
            throw new ServiceException("Lỗi khi đọc bàn.", e);
        }
    }

    public long add(String tableCode, int capacity) throws ServiceException {
        validateCode(tableCode);
        validateCapacity(capacity);
        String code = tableCode.trim();
        try {
            if (diningTableDAO.existsTableCode(code, null)) {
                throw new ServiceException("Mã bàn \"" + code + "\" đã tồn tại.");
            }
            return diningTableDAO.insert(code, capacity);
        } catch (SQLException e) {
            throw new ServiceException("Không thêm được bàn.", e);
        }
    }

    public void update(long id, String tableCode, int capacity) throws ServiceException {
        validateCode(tableCode);
        validateCapacity(capacity);
        String code = tableCode.trim();
        try {
            if (diningTableDAO.findById(id).isEmpty()) {
                throw new ServiceException("Không tìm thấy bàn có id = " + id + ".");
            }
            if (diningTableDAO.existsTableCode(code, id)) {
                throw new ServiceException("Mã bàn \"" + code + "\" đã được bàn khác sử dụng.");
            }
            if (!diningTableDAO.update(id, code, capacity)) {
                throw new ServiceException("Cập nhật bàn thất bại.");
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi khi cập nhật bàn.", e);
        }
    }

    public void delete(long id) throws ServiceException {
        try {
            var opt = diningTableDAO.findById(id);
            if (opt.isEmpty()) {
                throw new ServiceException("Không tìm thấy bàn có id = " + id + ".");
            }
            DiningTable t = opt.get();
            if (t.getStatus() == TableStatus.OCCUPIED) {
                throw new ServiceException("Không xóa bàn đang có khách (OCCUPIED).");
            }
            if (diningTableDAO.countOrdersForTable(id) > 0) {
                throw new ServiceException("Không xóa được: bàn đã có lịch sử hóa đơn (ràng buộc CSDL).");
            }
            if (!diningTableDAO.delete(id)) {
                throw new ServiceException("Xóa bàn thất bại.");
            }
        } catch (SQLException e) {
            throw new ServiceException("Lỗi khi xóa bàn (có thể còn dữ liệu liên quan).", e);
        }
    }

    private void validateCode(String code) throws ServiceException {
        if (code == null || code.isBlank()) {
            throw new ServiceException("Mã bàn không được để trống.");
        }
        String c = code.trim();
        if (c.length() < CODE_MIN_LEN || c.length() > CODE_MAX_LEN) {
            throw new ServiceException("Mã bàn phải từ " + CODE_MIN_LEN + " đến " + CODE_MAX_LEN + " ký tự.");
        }
    }

    private void validateCapacity(int capacity) throws ServiceException {
        if (capacity < CAPACITY_MIN || capacity > CAPACITY_MAX) {
            throw new ServiceException("Sức chứa phải từ " + CAPACITY_MIN + " đến " + CAPACITY_MAX + ".");
        }
    }
}
