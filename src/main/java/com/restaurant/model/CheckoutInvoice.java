package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Dữ liệu in hóa đơn sau thanh toán. */
public class CheckoutInvoice {

    private long orderId;
    private String tableCode;
    private LocalDateTime checkedOutAt;
    private BigDecimal totalAmount;
    private List<OrderLineView> billableLines;

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public String getTableCode() {
        return tableCode;
    }

    public void setTableCode(String tableCode) {
        this.tableCode = tableCode;
    }

    public LocalDateTime getCheckedOutAt() {
        return checkedOutAt;
    }

    public void setCheckedOutAt(LocalDateTime checkedOutAt) {
        this.checkedOutAt = checkedOutAt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<OrderLineView> getBillableLines() {
        return billableLines;
    }

    public void setBillableLines(List<OrderLineView> billableLines) {
        this.billableLines = billableLines;
    }
}
