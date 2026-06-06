package com.syskewer.api.dto.salon;

import com.syskewer.api.model.salon.Table;

public record TableResponseDto(
        Integer id,
        Integer number,
        Boolean occupied
) {
    public TableResponseDto(Table table) {
        this(table.getId(), table.getNumber(), table.getOccupied());
    }
}