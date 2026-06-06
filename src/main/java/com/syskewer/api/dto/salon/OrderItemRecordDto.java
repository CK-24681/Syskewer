package com.syskewer.api.dto.salon;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record OrderItemRecordDto(
        @NotNull(message = "O ID do produto é obrigatório")
        Integer productId,
        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser maior que zero")
        Integer quantity,
        Boolean isToGo,
        String packagingInstructions,
        String notes,
        List<String> sideDishes
) {}
