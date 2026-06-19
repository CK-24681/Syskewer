package com.syskewer.api.dto.salon;

import java.math.BigDecimal;
import java.util.List;

public record ComandaPartialDto(
        Integer comandaId,
        String customerName,
        BigDecimal totalAmount,
        List<PartialItemDto> timeline,
        List<PartialItemDto> groupedItems
) {}
