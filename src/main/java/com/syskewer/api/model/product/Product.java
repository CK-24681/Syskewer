package com.syskewer.api.model.product;

import java.math.BigDecimal;

import com.syskewer.api.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity 
@Table(name = "tb_product") 
public class Product extends BaseEntity<Integer> {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "prep_location_id")
    private PrepLocation prepLocation;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "in_stock", nullable = false)
    private Boolean inStock;

    public Product() {}

    public Product(String name, BigDecimal price, PrepLocation prepLocation, Category category, Boolean active, Boolean inStock) {
        this.name = name;
        this.price = price;
        this.prepLocation = prepLocation;
        this.category = category;
        this.active = active;
        this.inStock = inStock;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public PrepLocation getPrepLocation() { return prepLocation; }
    public void setPrepLocation(PrepLocation prepLocation) { this.prepLocation = prepLocation; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getInStock() { return inStock; }
    public void setInStock(Boolean inStock) { this.inStock = inStock; }
}