package com.syskewer.api.model.salon;

import java.time.LocalDateTime;

import com.syskewer.api.model.BaseEntity;
import com.syskewer.api.model.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@jakarta.persistence.Table(name = "tb_order")
public class Order extends BaseEntity<Long> {

    @ManyToOne
    @JoinColumn(name = "tab_id", nullable = false)
    private Tab tab;

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

    public Order() {}

    public Tab getTab() { return tab; }
    public void setTab(Tab tab) { this.tab = tab; }

    public User getWaiter() { return waiter; }
    public void setWaiter(User waiter) { this.waiter = waiter; }

    public PrepStatus getPrepStatus() { return prepStatus; }
    public void setPrepStatus(PrepStatus prepStatus) { this.prepStatus = prepStatus; }

    public OrderOrigin getOrigin() { return origin; }
    public void setOrigin(OrderOrigin origin) { this.origin = origin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}