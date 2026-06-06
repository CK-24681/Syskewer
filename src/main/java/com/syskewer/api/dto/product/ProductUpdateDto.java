package com.syskewer.api.dto.product;

import java.math.BigDecimal;

public record ProductUpdateDto(
        String name,
        BigDecimal price,
        Integer categoryId,
        Integer prepLocationId,
        Boolean inStock) {
}
