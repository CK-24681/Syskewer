package com.syskewer.api.model.salon;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.syskewer.api.model.BaseEntity;
import com.syskewer.api.model.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_comanda_item")
public class ComandaItem extends BaseEntity<Long> {

    @ManyToOne
    @JoinColumn(name = "comanda_id", nullable = false)
    @JsonIgnore
    private Comanda comanda;

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

    public ComandaItem() {}

    public Comanda getComanda() { return comanda; }
    public void setComanda(Comanda comanda) { this.comanda = comanda; }

    public User getWaiter() { return waiter; }
    public void setWaiter(User waiter) { this.waiter = waiter; }

    public PrepStatus getPrepStatus() { return prepStatus; }
    public void setPrepStatus(PrepStatus prepStatus) { this.prepStatus = prepStatus; }

    public OrderOrigin getOrigin() { return origin; }
    public void setOrigin(OrderOrigin origin) { this.origin = origin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
