package com.syskewer.api.model.salon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.syskewer.api.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@jakarta.persistence.Table(name = "tb_tab")
public class Tab extends BaseEntity<Integer> {

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private Table table;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TabStatus status = TabStatus.OPEN;

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

    public Tab() {}

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Table getTable() { return table; }
    public void setTable(Table table) { this.table = table; }

    public TabStatus getStatus() { return status; }
    public void setStatus(TabStatus status) { this.status = status; }

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