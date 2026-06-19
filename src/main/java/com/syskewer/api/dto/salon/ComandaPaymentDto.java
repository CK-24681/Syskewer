package com.syskewer.api.dto.salon;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ComandaPaymentDto(
        @NotNull(message = "O valor do pagamento é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal amount,
        @PositiveOrZero(message = "O valor do desconto deve ser maior ou igual a zero")
        BigDecimal discount
) {}
