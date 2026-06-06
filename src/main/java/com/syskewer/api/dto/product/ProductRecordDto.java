package com.syskewer.api.dto.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductRecordDto(
        @NotBlank(message = "O nome do produto é obrigatório") String name,

        @NotNull(message = "O preço é obrigatório") @Positive(message = "O preço deve ser maior que zero") BigDecimal price,

        @NotNull(message = "A categoria é obrigatória") Integer categoryId,

        Integer prepLocationId,

        Boolean inStock) {
}
