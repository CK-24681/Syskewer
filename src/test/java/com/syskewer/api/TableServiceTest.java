package com.syskewer.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syskewer.api.dto.salon.TableRecordDto;
import com.syskewer.api.dto.salon.TableResponseDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.model.salon.Table;
import com.syskewer.api.repository.salon.TableRepository;
import com.syskewer.api.service.salon.TableService;

@ExtendWith(MockitoExtension.class)
class TableServiceTest {

    @Mock private TableRepository tableRepository;

    @InjectMocks private TableService tableService;

    @Test
    @DisplayName("Deve criar uma mesa nova no salão com sucesso")
    void createTable_Success() {
        TableRecordDto dto = new TableRecordDto(5); 
        
        when(tableRepository.existsByNumber(5)).thenReturn(false);
        when(tableRepository.save(any(Table.class))).thenAnswer(i -> {
            Table t = (Table) i.getArguments()[0];
            t.setId(1);
            return t;
        });

        TableResponseDto response = tableService.createTable(dto);

        assertEquals(5, response.number());
        assertEquals(false, response.occupied());
        verify(tableRepository).save(any(Table.class));
    }

    @Test
    @DisplayName("Deve impedir a criação de uma mesa com número duplicado")
    void createTable_ThrowsException_WhenNumberExists() {
        TableRecordDto dto = new TableRecordDto(5); 
        
        when(tableRepository.existsByNumber(5)).thenReturn(true);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> tableService.createTable(dto));
        assertNotNull(exception);
    }
}