package com.syskewer.api.dto.product;

import com.syskewer.api.model.product.PrepLocation;

public record PrepLocationResponseDto(
    Integer id,
    String name
) {
    public PrepLocationResponseDto(PrepLocation location) {
        this(
            location.getId(),
            location.getName()
        );
    }
}
