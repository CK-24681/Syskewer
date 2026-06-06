package com.syskewer.api.dto.salon;

import com.syskewer.api.model.salon.ConsumptionType;

import jakarta.validation.constraints.NotNull;

public record TabOpenDto(
        String customerName,
        Integer tableNumber,
        @NotNull(message = "O tipo de consumo é obrigatório")
        ConsumptionType consumptionType
) {}
