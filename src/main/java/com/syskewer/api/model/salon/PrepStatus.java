package com.syskewer.api.model.salon;

/** Ciclo do pedido na cozinha — PREPARING bloqueia edição pelo garçom. */
public enum PrepStatus {
    QUEUED,
    PREPARING,
    READY,
    DELIVERED
}
