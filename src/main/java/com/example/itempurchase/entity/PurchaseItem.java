package com.example.itempurchase.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_pi_purchase_item", columnNames = {"purchase_id", "item_id"}))
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    // items 1 -> many purchase_items
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PurchaseItem() { }

    public PurchaseItem(Purchase purchase, Item item, Integer quantity) {
        this.purchase = purchase;
        this.item = item;
        this.quantity = quantity;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Purchase getPurchase() { return purchase; }
    public Item getItem() { return item; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
