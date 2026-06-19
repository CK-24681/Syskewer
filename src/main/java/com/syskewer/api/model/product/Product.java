package com.syskewer.api.model.product;

import com.syskewer.api.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity 
@Table(name = "tb_product") 
public class Product extends BaseEntity<Integer> {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "in_stock", nullable = false)
    private Boolean inStock;

    public Product() {}

    public Product(String name, Boolean active, Boolean inStock) {
        this.name = name;
        this.active = active;
        this.inStock = inStock;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getInStock() { return inStock; }
    public void setInStock(Boolean inStock) { this.inStock = inStock; }
}