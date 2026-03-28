package com.restaurant.model.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Một dòng doanh thu theo ngày (orders PAID). */
public record RevenueByDayRow(LocalDate day, BigDecimal totalAmount, int orderCount) {
}
