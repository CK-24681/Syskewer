package com.syskewer.api.dto.salon;

import java.time.LocalDateTime;
import java.util.List;

public record KitchenComandaItemDto(
        Long comandaItemId,
        String origin,
        String destination,
        String status,
        LocalDateTime createdAt,
        String waiterName,
        List<KitchenItemDto> items
) {}
