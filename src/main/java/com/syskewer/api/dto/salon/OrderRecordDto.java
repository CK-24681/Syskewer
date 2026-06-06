package com.syskewer.api.dto.salon;

import java.util.List;

import com.syskewer.api.model.salon.OrderOrigin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record OrderRecordDto(
        @NotNull(message = "O ID da comanda (Tab) é obrigatório")
        Integer tabId,
        OrderOrigin origin,
        @NotEmpty(message = "O pedido deve conter pelo menos um item")
        List<OrderItemRecordDto> items
) {}
