package com.syskewer.api.dto.salon;

import java.math.BigDecimal;

public record PartialItemDto(
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        Boolean isToGo
) {}