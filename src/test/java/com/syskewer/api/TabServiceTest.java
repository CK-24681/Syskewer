package com.syskewer.api;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syskewer.api.dto.salon.TabOpenDto;
import com.syskewer.api.dto.salon.TabSummaryDto;
import com.syskewer.api.model.salon.ConsumptionType;
import com.syskewer.api.model.salon.Tab;
import com.syskewer.api.model.salon.Table;
import com.syskewer.api.repository.salon.OrderRepository;
import com.syskewer.api.repository.salon.TabRepository;
import com.syskewer.api.repository.salon.TableRepository;
import com.syskewer.api.service.salon.TabService;

@ExtendWith(MockitoExtension.class)
class TabServiceTest {

    @Mock
    private TabRepository tabRepository;

    @Mock
    private TableRepository tableRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private TabService tabService;

    @Test
    @DisplayName("Deve abrir uma comanda em uma mesa livre")
    void openTab_Success() {
        // O cliente chega no salão e solicita uma mesa
        TabOpenDto dto = new TabOpenDto("Cliente Carlos", 1, ConsumptionType.MESA);
        
        Table table = new Table();
        table.setId(1);
        table.setNumber(1);
        table.setOccupied(false); 

        when(tableRepository.findByNumber(1)).thenReturn(Optional.of(table));
        
        when(tabRepository.save(any(Tab.class))).thenAnswer(i -> {
            Tab t = (Tab) i.getArguments()[0];
            t.setId(100);
            return t;
        });

        // O garçom inicia o atendimento. A mesa física é reservada e a conta é atrelada a ela.
        TabSummaryDto result = tabService.openTab(dto);

        assertNotNull(result);
        assertEquals("Cliente Carlos", result.customerName());
        assertEquals(1, result.tableNumber());
        assertTrue(table.getOccupied()); 
        
        verify(tableRepository, times(1)).save(table);
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Deve lançar erro se a mesa informada não existir")
    void openTab_ThrowsException_WhenTableNotFound() {
        // Sistema Anti-Fraude do Salão: Impede que o garçom lance uma comanda 
        // para uma "Mesa 99" que sequer existe na planta física do restaurante.
        TabOpenDto dto = new TabOpenDto("Cliente Invasor", 99, ConsumptionType.MESA);

        when(tableRepository.findByNumber(99)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tabService.openTab(dto);
        });

        assertTrue(exception.getMessage().contains("não encontrada no salão"));
        verify(tabRepository, never()).save(any(Tab.class));
    }
}