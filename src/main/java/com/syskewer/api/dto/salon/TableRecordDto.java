package com.syskewer.api.dto.salon;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TableRecordDto(
        @NotNull(message = "O número da mesa é obrigatório")
        @Positive(message = "O número da mesa deve ser maior que zero")
        Integer number
) {}