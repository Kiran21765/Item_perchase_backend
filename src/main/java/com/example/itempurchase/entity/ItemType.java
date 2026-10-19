package com.example.itempurchase.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item_types")
public class ItemType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_name", nullable = false, unique = true, length = 100)
    private String typeName;

    // item_types 1 -> many items
    @OneToMany(mappedBy = "itemType")
    private List<Item> items = new ArrayList<>();

    public Long getId() { return id; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
}
