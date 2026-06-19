package com.syskewer.api.model.salon;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.syskewer.api.model.BaseEntity;
import com.syskewer.api.model.product.Menu;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_comanda_item_detail")
public class ComandaItemDetail extends BaseEntity<Long> {

    @ManyToOne
    @JoinColumn(name = "comanda_item_id", nullable = false)
    @JsonIgnore
    private ComandaItem comandaItem;

    @ManyToOne
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "sold_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal soldPrice;

    @Column(name = "is_to_go", nullable = false)
    private Boolean isToGo = false;

    @Column(name = "packaging_instructions", length = 255)
    private String packagingInstructions;

    @Column(length = 255)
    private String notes;

    @ElementCollection
    @CollectionTable(name = "tb_order_item_sides", joinColumns = @JoinColumn(name = "order_item_id"))
    @Column(name = "side_dish")
    private List<String> sideDishes = new ArrayList<>();

    public ComandaItemDetail() {}

    public ComandaItem getComandaItem() { return comandaItem; }
    public void setComandaItem(ComandaItem comandaItem) { this.comandaItem = comandaItem; }

    public Menu getMenu() { return menu; }
    public void setMenu(Menu menu) { this.menu = menu; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getSoldPrice() { return soldPrice; }
    public void setSoldPrice(BigDecimal soldPrice) { this.soldPrice = soldPrice; }

    public Boolean getIsToGo() { return isToGo; }
    public void setIsToGo(Boolean toGo) { isToGo = toGo; }

    public String getPackagingInstructions() { return packagingInstructions; }
    public void setPackagingInstructions(String packagingInstructions) { this.packagingInstructions = packagingInstructions; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<String> getSideDishes() { return sideDishes; }
    public void setSideDishes(List<String> sideDishes) { this.sideDishes = sideDishes; }
}
