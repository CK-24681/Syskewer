package com.syskewer.api.dto.salon;

import java.math.BigDecimal;
import java.util.List;

public record BillSummaryDto(
        Integer id,
        String customerName,
        List<Integer> tableNumbers,
        String consumptionType,
        BigDecimal totalAmount
) {}
