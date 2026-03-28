package com.restaurant.service;

import com.restaurant.dao.ReportDAO;
import com.restaurant.model.report.RevenueByDayRow;
import com.restaurant.model.report.TopDishRow;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {

    private static final int TOP_MIN = 1;
    private static final int TOP_MAX = 50;

    private final ReportDAO reportDAO = new ReportDAO();

    public BigDecimal totalRevenuePaidBetween(LocalDate from, LocalDate to) throws ServiceException {
        validateRange(from, to);
        try {
            return reportDAO.sumRevenuePaidBetween(from, to);
        } catch (SQLException e) {
            throw new ServiceException("Không đọc được doanh thu.", e);
        }
    }

    public List<RevenueByDayRow> revenueByDayPaidBetween(LocalDate from, LocalDate to) throws ServiceException {
        validateRange(from, to);
        try {
            return reportDAO.revenueByDayPaidBetween(from, to);
        } catch (SQLException e) {
            throw new ServiceException("Không đọc được doanh thu theo ngày.", e);
        }
    }

    public List<TopDishRow> topDishesByQuantity(LocalDate from, LocalDate to, int topN) throws ServiceException {
        validateRange(from, to);
        try {
            return reportDAO.topDishesByQuantity(from, to, clampTop(topN));
        } catch (SQLException e) {
            throw new ServiceException("Không đọc được top món (số lượng).", e);
        }
    }

    public List<TopDishRow> topDishesByRevenue(LocalDate from, LocalDate to, int topN) throws ServiceException {
        validateRange(from, to);
        try {
            return reportDAO.topDishesByRevenue(from, to, clampTop(topN));
        } catch (SQLException e) {
            throw new ServiceException("Không đọc được top món (doanh thu).", e);
        }
    }

    /** Khoảng [đầu tháng, cuối tháng] theo YearMonth. */
    public LocalDate[] monthBounds(YearMonth ym) {
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        return new LocalDate[]{from, to};
    }

    private static void validateRange(LocalDate from, LocalDate to) throws ServiceException {
        if (from == null || to == null) {
            throw new ServiceException("Ngày không được để trống.");
        }
        if (from.isAfter(to)) {
            throw new ServiceException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
    }

    private static int clampTop(int n) {
        if (n < TOP_MIN) {
            return 10;
        }
        return Math.min(n, TOP_MAX);
    }
}
