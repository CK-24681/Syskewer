package com.syskewer.api.dto.salon;

import java.math.BigDecimal;

public record TabSummaryDto(
        Integer id,
        String customerName,
        Integer tableNumber,
        String consumptionType,
        BigDecimal totalAmount
) {}
