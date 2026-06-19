package com.syskewer.api.dto.product;

import java.math.BigDecimal;
import com.syskewer.api.model.product.Menu;

public record MenuResponseDto(
        Integer id,
        String name,
        BigDecimal price,
        String categoryName,
        String prepLocationName,
        Boolean inStock,
        Boolean active
) {
    public MenuResponseDto(Menu menu) {
        this(
            menu.getId(),
            menu.getName(),
            menu.getPrice(),
            menu.getCategory() != null ? menu.getCategory().getName() : null,
            menu.getPrepLocation() != null ? menu.getPrepLocation().getName() : null,
            menu.getProducts().isEmpty() || menu.getProducts().stream().allMatch(p -> Boolean.TRUE.equals(p.getInStock())),
            menu.getActive()
        );
    }
}
