package com.syskewer.api.dto.salon;

import java.math.BigDecimal;
import java.util.List;

public record TabPartialDto(
        Integer tabId,
        String customerName,
        BigDecimal totalAmount,
        List<PartialItemDto> items
) {}