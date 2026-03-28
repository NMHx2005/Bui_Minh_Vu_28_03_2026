package com.restaurant.model.report;

import java.math.BigDecimal;

/** Món bán chạy (aggregate từ order_details + orders PAID). */
public record TopDishRow(long menuItemId, String menuItemName, long totalQuantity, BigDecimal totalRevenue) {
}
