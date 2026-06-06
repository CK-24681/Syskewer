package com.syskewer.api.model.store;

import com.syskewer.api.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_store_settings")
public class StoreSettings extends BaseEntity<Integer> {

    @Column(name = "is_open", nullable = false)
    private Boolean isOpen;

    public StoreSettings() {}

    public StoreSettings(Boolean isOpen) {
        this.isOpen = isOpen;
    }

    public Boolean getIsOpen() { return isOpen; }
    public void setIsOpen(Boolean isOpen) { this.isOpen = isOpen; }
}