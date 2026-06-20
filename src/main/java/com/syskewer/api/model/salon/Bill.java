package com.syskewer.api.model.salon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.syskewer.api.model.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_bill")
public class Bill extends BaseEntity<Integer> {

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @ManyToMany
    @JoinTable(
        name = "tb_bill_table",
        joinColumns = @JoinColumn(name = "bill_id"),
        inverseJoinColumns = @JoinColumn(name = "table_id")
    )
    private List<com.syskewer.api.model.salon.Table> tables = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Order> orders = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillStatus status = BillStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "consumption_type", nullable = false, length = 20)
    private ConsumptionType consumptionType;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "customer_doc_or_phone", length = 100)
    private String customerDocOrPhone;

    @Column(name = "apply_cover_charge", nullable = false)
    private Boolean applyCoverCharge = false;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt = LocalDateTime.now();

    @Column(name = "deferred_date")
    private LocalDateTime deferredDate;

    public Bill() {}

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public List<com.syskewer.api.model.salon.Table> getTables() { return tables; }
    public void setTables(List<com.syskewer.api.model.salon.Table> tables) { this.tables = tables; }

    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }

    public BillStatus getStatus() { return status; }
    public void setStatus(BillStatus status) { this.status = status; }

    public ConsumptionType getConsumptionType() { return consumptionType; }
    public void setConsumptionType(ConsumptionType consumptionType) { this.consumptionType = consumptionType; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public Boolean getApplyCoverCharge() { return applyCoverCharge; }
    public void setApplyCoverCharge(Boolean applyCoverCharge) { this.applyCoverCharge = applyCoverCharge; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public LocalDateTime getDeferredDate() { return deferredDate; }
    public void setDeferredDate(LocalDateTime deferredDate) { this.deferredDate = deferredDate; }

    public String getCustomerDocOrPhone() { return customerDocOrPhone; }
    public void setCustomerDocOrPhone(String customerDocOrPhone) { this.customerDocOrPhone = customerDocOrPhone; }
}
