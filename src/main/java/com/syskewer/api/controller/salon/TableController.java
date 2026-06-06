package com.syskewer.api.controller.salon;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syskewer.api.dto.salon.TableRecordDto;
import com.syskewer.api.dto.salon.TableResponseDto;
import com.syskewer.api.service.salon.TableService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/tables")
public class TableController {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    /** @param dto número da mesa */
    @PostMapping
    public ResponseEntity<TableResponseDto> create(@RequestBody @Valid TableRecordDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableService.createTable(dto));
    }

    /** @return mapa de mesas com status de ocupação */
    @GetMapping
    public ResponseEntity<List<TableResponseDto>> list() {
        return ResponseEntity.ok(tableService.listAll());
    }
}
