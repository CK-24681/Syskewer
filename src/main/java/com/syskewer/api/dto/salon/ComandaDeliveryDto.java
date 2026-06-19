package com.syskewer.api.dto.salon;

import jakarta.validation.constraints.NotBlank;

public record ComandaDeliveryDto(
        @NotBlank(message = "O nome do cliente é obrigatório no Delivery.")
        String customerName,

        @NotBlank(message = "O endereço de entrega é obrigatório para despachar o pedido.")
        String deliveryAddress
) {}
