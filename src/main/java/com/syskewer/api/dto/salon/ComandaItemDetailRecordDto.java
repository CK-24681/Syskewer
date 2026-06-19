package com.syskewer.api.dto.salon;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record ComandaItemDetailRecordDto(
        @NotNull(message = "O ID do item de cardápio é obrigatório")
        Integer menuId,
        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser maior que zero")
        Integer quantity,
        Boolean isToGo,
        String packagingInstructions,
        String notes,
        List<String> sideDishes
) {}
