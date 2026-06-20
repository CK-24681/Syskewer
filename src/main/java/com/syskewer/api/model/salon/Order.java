package com.syskewer.api.model.salon;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.syskewer.api.model.BaseEntity;
import com.syskewer.api.model.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_order")
public class Order extends BaseEntity<Long> {

    @ManyToOne
    @JoinColumn(name = "bill_id", nullable = false)
    @JsonIgnore
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waiter_id")
    private User waiter;

    @Enumerated(EnumType.STRING)
    @Column(name = "prep_status", nullable = false)
    private PrepStatus prepStatus = PrepStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderOrigin origin = OrderOrigin.WAITER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<OrderDetail> details = new ArrayList<>();

    public Order() {}

    public Bill getBill() { return bill; }
    public void setBill(Bill bill) { this.bill = bill; }

    public User getWaiter() { return waiter; }
    public void setWaiter(User waiter) { this.waiter = waiter; }

    public PrepStatus getPrepStatus() { return prepStatus; }
    public void setPrepStatus(PrepStatus prepStatus) { this.prepStatus = prepStatus; }

    public OrderOrigin getOrigin() { return origin; }
    public void setOrigin(OrderOrigin origin) { this.origin = origin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<OrderDetail> getDetails() { return details; }
    public void setDetails(List<OrderDetail> details) { this.details = details; }
}
