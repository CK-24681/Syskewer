package com.syskewer.api.dto.product;

import java.math.BigDecimal;

import com.syskewer.api.model.product.Product;

public record ProductResponseDto(
        Integer id,
        String name,
        BigDecimal price,
        String categoryName,
        String prepLocationName,
        Boolean inStock,
        Boolean active
) {
    public ProductResponseDto(Product product) {
        this(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getCategory() != null ? product.getCategory().getName() : null,
            product.getPrepLocation() != null ? product.getPrepLocation().getName() : null,
            product.getInStock(),
            product.getActive()
        );
    }
}