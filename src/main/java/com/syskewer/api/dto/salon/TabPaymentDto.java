package com.syskewer.api.dto.salon;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TabPaymentDto(
        @NotNull(message = "O valor do pagamento é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal amount,
        BigDecimal discount
) {}