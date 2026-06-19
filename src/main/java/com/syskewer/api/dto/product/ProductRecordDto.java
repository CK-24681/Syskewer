package com.syskewer.api.dto.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductRecordDto(
    @Schema(example = "Cerveja Heineken 600ml")
    @NotBlank(message = "O nome do produto é obrigatório")
    String name,

    @Schema(example = "15.90")
    @NotNull(message = "O preço é obrigatório")
    @Positive(message = "O preço deve ser maior que zero")
    BigDecimal price,

    @NotNull(message = "O ID da categoria é obrigatório")
    Integer categoryId,

    Integer prepLocationId,

    @NotNull(message = "Informe se o produto está em estoque")
    Boolean inStock
) {}