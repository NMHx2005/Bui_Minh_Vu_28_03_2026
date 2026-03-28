package com.restaurant.model;

import java.time.LocalDateTime;

/**
 * Một dòng trong hàng đợi bếp (join order_details + orders + bàn + món).
 */
public class ChefKitchenLine {

    private long detailId;
    private long orderId;
    private String tableCode;
    private String menuItemName;
    private int quantity;
    private OrderLineStatus lineStatus;
    private MenuItemType itemType;
    private ManagerApproval managerApproval;
    private LocalDateTime createdAt;

    public long getDetailId() {
        return detailId;
    }

    public void setDetailId(long detailId) {
        this.detailId = detailId;
    }

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

    public String getMenuItemName() {
        return menuItemName;
    }

    public void setMenuItemName(String menuItemName) {
        this.menuItemName = menuItemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public OrderLineStatus getLineStatus() {
        return lineStatus;
    }

    public void setLineStatus(OrderLineStatus lineStatus) {
        this.lineStatus = lineStatus;
    }

    public MenuItemType getItemType() {
        return itemType;
    }

    public void setItemType(MenuItemType itemType) {
        this.itemType = itemType;
    }

    public ManagerApproval getManagerApproval() {
        return managerApproval;
    }

    public void setManagerApproval(ManagerApproval managerApproval) {
        this.managerApproval = managerApproval;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
