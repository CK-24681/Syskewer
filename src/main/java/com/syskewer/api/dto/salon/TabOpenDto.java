package com.syskewer.api.dto.salon;

import com.syskewer.api.model.salon.ConsumptionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TabOpenDto(
    @NotBlank(message = "O nome do cliente é obrigatório")
    String customerName,

    @NotNull(message = "O número da mesa é obrigatório")
    @Positive(message = "O número da mesa deve ser maior que zero")
    Integer tableNumber,

    @NotNull(message = "O tipo de consumo (MESA, BALCAO, etc) é obrigatório")
    ConsumptionType consumptionType
) {}