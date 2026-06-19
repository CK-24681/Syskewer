package com.syskewer.api.dto.product;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequestDto(
        @NotBlank(message = "O nome da categoria é obrigatório") String name,
        Integer parentId
) {}