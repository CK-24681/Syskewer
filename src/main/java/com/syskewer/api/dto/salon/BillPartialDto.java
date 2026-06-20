package com.syskewer.api.dto.salon;

import java.math.BigDecimal;
import java.util.List;

public record BillPartialDto(
        Integer billId,
        String customerName,
        BigDecimal totalAmount,
        List<PartialItemDto> timeline,
        List<PartialItemDto> groupedItems
) {}
