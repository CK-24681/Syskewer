package com.syskewer.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.syskewer.api.dto.salon.ComandaOpenDto;
import com.syskewer.api.dto.salon.ComandaSummaryDto;
import com.syskewer.api.dto.salon.ComandaUpdateDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.model.salon.ConsumptionType;
import com.syskewer.api.model.salon.Comanda;
import com.syskewer.api.model.salon.ComandaStatus;
import com.syskewer.api.model.salon.Table;
import com.syskewer.api.repository.salon.ComandaRepository;
import com.syskewer.api.repository.salon.TableRepository;
import com.syskewer.api.service.salon.StoreSettingsService;
import com.syskewer.api.service.salon.ComandaService;

@ExtendWith(MockitoExtension.class)
class ComandaServiceTest {

    @Mock private ComandaRepository comandaRepository;
    @Mock private TableRepository tableRepository;
    @Mock private StoreSettingsService storeSettingsService;

    @InjectMocks private ComandaService comandaService;

    private void mockSecurityRole(String role) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        org.mockito.Mockito.<java.util.Collection<? extends GrantedAuthority>>when(auth.getAuthorities()).thenReturn(authorities);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    @DisplayName("Abertura com Nome Automático se garçom não digitar")
    void openTab_AutoNames() {
        when(storeSettingsService.isStoreOpen()).thenReturn(true);
        ComandaOpenDto dto = new ComandaOpenDto("", 4, ConsumptionType.MESA);
        
        Table table = new Table(); table.setId(4); table.setNumber(4); table.setOccupied(false);
        when(tableRepository.findByNumber(4)).thenReturn(Optional.of(table));
        
        when(comandaRepository.findByTableIdAndStatus(any(), any())).thenReturn(List.of(new Comanda()));
        
        when(comandaRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        ComandaSummaryDto result = comandaService.openTab(dto);

        assertEquals("Mesa 4 - Cliente 2", result.customerName());
    }

    @Test
    @DisplayName("Pagamento com Desconto falha se não for Admin")
    void registerPayment_WithDiscount_ThrowsIfGarcom() {
        mockSecurityRole("ROLE_GARCOM");

        Comanda comanda = new Comanda();
        comanda.setTotalAmount(new BigDecimal("105.00"));
        comanda.setStatus(ComandaStatus.OPEN);

        when(comandaRepository.findById(1)).thenReturn(Optional.of(comanda));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> 
            comandaService.registerPayment(1, new BigDecimal("100.00"), new BigDecimal("5.00"))
        );
        assertNotNull(exception);
    }

    @Test
    @DisplayName("Pagamento Agrupado de 2 comandas liquidando tudo")
    void payGroupedTabs_Success() {
        Comanda comanda1 = new Comanda(); comanda1.setId(1); comanda1.setTotalAmount(new BigDecimal("50.00")); comanda1.setStatus(ComandaStatus.OPEN);
        Comanda comanda2 = new Comanda(); comanda2.setId(2); comanda2.setTotalAmount(new BigDecimal("30.00")); comanda2.setStatus(ComandaStatus.OPEN);

        when(comandaRepository.findAllById(List.of(1, 2))).thenReturn(List.of(comanda1, comanda2));

        comandaService.payGroupedTabs(List.of(1, 2), new BigDecimal("80.00"));

        assertEquals(ComandaStatus.CLOSED, comanda1.getStatus());
        assertEquals(ComandaStatus.CLOSED, comanda2.getStatus());
        assertEquals(new BigDecimal("50.00"), comanda1.getPaidAmount());
        assertEquals(new BigDecimal("30.00"), comanda2.getPaidAmount());
    }

    @Test
    @DisplayName("Reabrir comanda (Mesa antiga já está ocupada por outra pessoa)")
    void reopenTab_Safely() {
        Table table5 = new Table(); table5.setNumber(5); table5.setOccupied(true);

        Comanda oldTab = new Comanda(); 
        oldTab.setStatus(ComandaStatus.CLOSED); 
        oldTab.getTables().add(table5);
        oldTab.setCustomerName("Cliente Antigo");

        when(comandaRepository.findById(1)).thenReturn(Optional.of(oldTab));

        comandaService.reopenTab(1);

        assertEquals(ComandaStatus.OPEN, oldTab.getStatus());
        assertEquals(0, oldTab.getTables().size()); 
    }

    @Test
    @DisplayName("Atualizar nome do cliente da comanda com patchTab")
    void patchTab_UpdatesCustomerName() {
        Comanda comanda = new Comanda();
        comanda.setId(1);
        comanda.setCustomerName("Cliente Original");
        comanda.setConsumptionType(ConsumptionType.MESA);

        when(comandaRepository.findById(1)).thenReturn(Optional.of(comanda));
        when(comandaRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        ComandaUpdateDto dto = new ComandaUpdateDto("Cliente Atualizado");
        ComandaSummaryDto result = comandaService.patchTab(1, dto);

        assertEquals("Cliente Atualizado", result.customerName());
        assertEquals("Cliente Atualizado", comanda.getCustomerName());
    }

    @Test
    @DisplayName("Pagamento com desconto maior que o saldo falha")
    void registerPayment_DiscountGreaterThanBalance_Throws() {
        mockSecurityRole("ROLE_ADMINISTRADOR");
        Comanda comanda = new Comanda();
        comanda.setTotalAmount(new BigDecimal("50.00"));
        comanda.setPaidAmount(new BigDecimal("10.00")); 
        comanda.setStatus(ComandaStatus.OPEN);

        when(comandaRepository.findById(1)).thenReturn(Optional.of(comanda));

        assertThrows(BusinessRuleException.class, () -> 
            comandaService.registerPayment(1, new BigDecimal("10.00"), new BigDecimal("45.00"))
        );
    }

    @Test
    @DisplayName("Arquivar comanda sem saldo devedor falha")
    void archiveAsCredit_NoBalance_Throws() {
        Comanda comanda = new Comanda();
        comanda.setTotalAmount(new BigDecimal("50.00"));
        comanda.setPaidAmount(new BigDecimal("50.00"));
        comanda.setStatus(ComandaStatus.OPEN);

        when(comandaRepository.findById(1)).thenReturn(Optional.of(comanda));

        assertThrows(BusinessRuleException.class, () -> 
            comandaService.archiveAsCredit(1, "123456789")
        );
    }

    @Test
    @DisplayName("Cancelar comanda fechada falha")
    void cancelTab_ClosedTab_Throws() {
        Comanda comanda = new Comanda();
        comanda.setStatus(ComandaStatus.CLOSED);

        when(comandaRepository.findById(1)).thenReturn(Optional.of(comanda));

        assertThrows(BusinessRuleException.class, () -> 
            comandaService.cancelTab(1)
        );
    }

    @Test
    @DisplayName("Transferir comanda fechada falha")
    void transferTab_ClosedTab_Throws() {
        Comanda comanda = new Comanda();
        comanda.setStatus(ComandaStatus.CLOSED);

        when(comandaRepository.findById(1)).thenReturn(Optional.of(comanda));

        assertThrows(BusinessRuleException.class, () -> 
            comandaService.transferTab(1, 2)
        );
    }

    @Test
    @DisplayName("Pagamento agrupado com valor zerado/negativo falha")
    void payGroupedTabs_ZeroAmount_Throws() {
        assertThrows(BusinessRuleException.class, () -> 
            comandaService.payGroupedTabs(List.of(1, 2), BigDecimal.ZERO)
        );
    }
}
