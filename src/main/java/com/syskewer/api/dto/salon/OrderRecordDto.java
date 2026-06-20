package com.syskewer.api.dto.salon;

import java.util.List;

import com.syskewer.api.model.salon.OrderOrigin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRecordDto(
    @NotNull(message = "O ID da conta é obrigatório")
    @Positive(message = "O ID da conta deve ser maior que zero")
    Integer billId,

    @NotNull(message = "A origem do pedido é obrigatória")
    OrderOrigin origin,

    @NotEmpty(message = "O pedido deve conter pelo menos um item")
    @Valid 
    List<OrderDetailRecordDto> items
) {}
