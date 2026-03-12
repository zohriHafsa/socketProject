package com.chrionline.shared.models;

public class CartItem {

    private int cartItemId;
    private int  cartId;
    private Product product;
    private int quantity;
    private double unitPrice;

    public CartItem() {}

    public CartItem(int cartItemId, int cartId, Product product, int quantity, double unitPrice) {
        this.cartItemId = cartItemId;
        this.cartId = cartId;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getCartItemId() { return cartItemId; }
    public void setCartItemId(int cartItemId) { this.cartItemId = cartItemId; }

    public int getCartId() { return cartId; }
    public void setCartId(int cartId) { this.cartId = cartId; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double  getSubtotal() { return unitPrice * quantity; }

    @Override
    public String toString() {
        return "CartItem{product=" + (product != null ? product.getName() : "null")
                + ", qty=" + quantity + ", unitPrice=" + unitPrice + "}";
    }
}