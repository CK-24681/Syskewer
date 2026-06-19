package com.syskewer.api.dto.product;

import jakarta.validation.constraints.NotBlank;

public record PrepLocationRequestDto(
        @NotBlank(message = "O nome do local de preparo é obrigatório") String name
) {}