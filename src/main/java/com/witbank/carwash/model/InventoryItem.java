package com.witbank.carwash.model;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private int quantity;
    private int threshold;

    public InventoryItem() {}

    public InventoryItem(String name, int quantity, int threshold) {
        this.name = name;
        this.quantity = quantity;
        this.threshold = threshold;
    }

    // ── Getters & Setters ──────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) { this.threshold = threshold; }

    /** Returns true when stock is at or below the reorder threshold. */
    public boolean isLowStock() {
        return quantity <= threshold;
    }
}
