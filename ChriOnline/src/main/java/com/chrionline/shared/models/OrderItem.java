package com.chrionline.shared.models;

public class OrderItem {

    private int orderItemId;
    private String orderId;
    private Product product;
    private String productSnapshot;   // JSON string — snapshot at time of purchase
    private int quantity;
    private double unitPrice;

    public OrderItem() {}

    public OrderItem(int orderItemId, String orderId, Product product,
                     String productSnapshot, int quantity, double unitPrice) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.product = product;
        this.productSnapshot = productSnapshot;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getOrderItemId() { return orderItemId; }
    public void setOrderItemId(int orderItemId) { this.orderItemId = orderItemId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getProductSnapshot() { return productSnapshot; }
    public void setProductSnapshot(String productSnapshot){ this.productSnapshot = productSnapshot; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getSubtotal() { return unitPrice * quantity; }

    @Override
    public String toString() {
        return "OrderItem{orderId='" + orderId + "', product="
                + (product != null ? product.getName() : productSnapshot)
                + ", qty=" + quantity + "}";
    }
}