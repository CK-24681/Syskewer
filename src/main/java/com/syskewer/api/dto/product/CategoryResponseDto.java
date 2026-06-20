package com.syskewer.api.dto.product;

import com.syskewer.api.model.product.Category;

public record CategoryResponseDto(
    Integer id,
    String name,
    Integer parentId
) {
    public CategoryResponseDto(Category category) {
        this(
            category.getId(),
            category.getName(),
            category.getParent() != null ? category.getParent().getId() : null
        );
    }
}
