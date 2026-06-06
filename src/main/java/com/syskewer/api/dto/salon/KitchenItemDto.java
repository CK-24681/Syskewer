package com.syskewer.api.dto.salon;

import java.util.List;

public record KitchenItemDto(
        String productName,
        Integer quantity,
        Boolean isToGo,
        String packagingInstructions,
        String notes,
        List<String> sideDishes
) {}
