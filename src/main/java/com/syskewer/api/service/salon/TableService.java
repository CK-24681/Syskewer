package com.syskewer.api.service.salon;

import java.util.List;

import org.springframework.stereotype.Service;

import com.syskewer.api.dto.salon.TableRecordDto;
import com.syskewer.api.dto.salon.TableResponseDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.model.salon.Table;
import com.syskewer.api.repository.salon.TableRepository;

// Servico para gerenciar as mesas do salao
@Service
public class TableService {

    private final TableRepository tableRepository;

    public TableService(TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    // Cadastra uma nova mesa validando se o numero ja existe
    public TableResponseDto createTable(TableRecordDto dto) {
        if (tableRepository.existsByNumber(dto.number())) {
            throw new BusinessRuleException("Já existe uma mesa cadastrada com o número " + dto.number());
        }

        Table table = new Table();
        table.setNumber(dto.number());
        table.setOccupied(false);

        table = tableRepository.save(table);
        return new TableResponseDto(table);
    }

    // Lista todas as mesas cadastradas
    public List<TableResponseDto> listAll() {
        return tableRepository.findAll().stream()
                .map(TableResponseDto::new)
                .toList();
    }
}