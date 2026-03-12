package com.chrionline.shared.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Cart {

    private int cartId;
    private User user;
    private List<CartItem> items;
    private LocalDateTime lastModified;

    public Cart() {
        this.items = new ArrayList<>();
    }

    public Cart(int cartId, User user, List<CartItem> items, LocalDateTime lastModified) {
        this.cartId = cartId;
        this.user = user;
        this.items = items != null ? items : new ArrayList<>();
        this.lastModified = lastModified;
    }

    public int getCartId() { return cartId; }
    public void setCartId(int cartId) { this.cartId = cartId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public double getTotal() {
        return items.stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    public int getItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public boolean isEmpty() { return items.isEmpty(); }

    @Override
    public String toString() {
        return "Cart{id=" + cartId + ", items=" + items.size() + ", total=" + getTotal() + "}";
    }
}
