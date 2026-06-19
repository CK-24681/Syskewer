package com.syskewer.api.dto.product;

import java.math.BigDecimal;
import jakarta.validation.constraints.Positive;

public record ProductUpdateDto(
        String name,
        @Positive(message = "O preço deve ser maior que zero")
        BigDecimal price,
        Integer categoryId,
        Integer prepLocationId,
        Boolean inStock) {
}
