package com.syskewer.api.dto.salon;

import java.time.LocalDateTime;
import java.util.List;

public record KitchenOrderDto(
        Long orderId,
        String origin,
        String destination,
        String status,
        LocalDateTime createdAt,
        List<KitchenItemDto> items
) {}
