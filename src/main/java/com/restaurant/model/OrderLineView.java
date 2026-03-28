package com.restaurant.model;

import java.math.BigDecimal;

/**
 * Một dòng hiển thị trên hóa đơn / theo dõi khách (join order_details + menu_items).
 */
public class OrderLineView {

    private long detailId;
    private long menuItemId;
    private String menuItemName;
    private MenuItemType itemType;
    private int quantity;
    private BigDecimal unitPrice;
    private OrderLineStatus lineStatus;
    private ManagerApproval managerApproval;

    public long getDetailId() {
        return detailId;
    }

    public void setDetailId(long detailId) {
        this.detailId = detailId;
    }

    public long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getMenuItemName() {
        return menuItemName;
    }

    public void setMenuItemName(String menuItemName) {
        this.menuItemName = menuItemName;
    }

    public MenuItemType getItemType() {
        return itemType;
    }

    public void setItemType(MenuItemType itemType) {
        this.itemType = itemType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public OrderLineStatus getLineStatus() {
        return lineStatus;
    }

    public void setLineStatus(OrderLineStatus lineStatus) {
        this.lineStatus = lineStatus;
    }

    public ManagerApproval getManagerApproval() {
        return managerApproval;
    }

    public void setManagerApproval(ManagerApproval managerApproval) {
        this.managerApproval = managerApproval;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
