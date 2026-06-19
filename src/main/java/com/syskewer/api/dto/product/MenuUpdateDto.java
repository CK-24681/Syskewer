package com.syskewer.api.dto.product;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.Positive;

public record MenuUpdateDto(
        String name,
        @Positive(message = "O preço deve ser maior que zero")
        BigDecimal price,
        Integer categoryId,
        Integer prepLocationId,
        List<Integer> productIds
) {}
