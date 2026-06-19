package com.syskewer.api.dto.product;

public record CategoryUpdateDto(
        String name,
        Integer parentId
) {}