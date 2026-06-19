package com.syskewer.api.dto.product;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;

public record MenuRecordDto(
    @Schema(example = "Cerveja Heineken 600ml")
    @NotBlank(message = "O nome do item é obrigatório")
    String name,

    @Schema(example = "15.90")
    @NotNull(message = "O preço é obrigatório")
    @Positive(message = "O preço deve ser maior que zero")
    BigDecimal price,

    @NotNull(message = "O ID da categoria é obrigatório")
    Integer categoryId,

    Integer prepLocationId,

    List<Integer> productIds
) {}
