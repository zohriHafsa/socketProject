package com.chrionline.shared.models;

import java.time.LocalDateTime;

public class Category {
    private int categoryId;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public Category() {}

    public Category(int categoryId, String name, String description, LocalDateTime createdAt) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String  getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Category{id=" + categoryId + ", name='" + name + "'}";
    }
}
