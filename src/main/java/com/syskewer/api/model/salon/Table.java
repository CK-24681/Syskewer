package com.syskewer.api.model.salon;

import com.syskewer.api.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@jakarta.persistence.Table(name = "tb_table")
public class Table extends BaseEntity<Integer> {

    @Column(nullable = false, unique = true)
    private Integer number;

    @Column(nullable = false)
    private Boolean occupied = false;

    public Table() {}

    public Table(Integer number, Boolean occupied) {
        this.number = number;
        this.occupied = occupied;
    }

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }

    public Boolean getOccupied() { return occupied; }
    public void setOccupied(Boolean occupied) { this.occupied = occupied; }
}