package com.syskewer.api.model.product;

import com.syskewer.api.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_prep_location")
public class PrepLocation extends BaseEntity<Integer> {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    public PrepLocation() {}

    public PrepLocation(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}