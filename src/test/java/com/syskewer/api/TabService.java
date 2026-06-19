package com.syskewer.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.syskewer.api.dto.salon.TabOpenDto;
import com.syskewer.api.dto.salon.TabSummaryDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.model.salon.ConsumptionType;
import com.syskewer.api.model.salon.Tab;
import com.syskewer.api.model.salon.TabStatus;
import com.syskewer.api.model.salon.Table;
import com.syskewer.api.repository.salon.TabRepository;
import com.syskewer.api.repository.salon.TableRepository;
import com.syskewer.api.service.salon.StoreSettingsService;
import com.syskewer.api.service.salon.TabService;

@ExtendWith(MockitoExtension.class)
class TabServiceTest {

    @Mock private TabRepository tabRepository;
    @Mock private TableRepository tableRepository;
    @Mock private StoreSettingsService storeSettingsService;

    @InjectMocks private TabService tabService;

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
        TabOpenDto dto = new TabOpenDto("", 4, ConsumptionType.MESA);
        
        Table table = new Table(); table.setNumber(4); table.setOccupied(false);
        when(tableRepository.findByNumber(4)).thenReturn(Optional.of(table));
        
        when(tabRepository.findByTableIdAndStatus(any(), any())).thenReturn(List.of(new Tab()));
        
        when(tabRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        TabSummaryDto result = tabService.openTab(dto);

        assertEquals("Mesa 4 - Cliente 2", result.customerName());
    }

    @Test
    @DisplayName("Pagamento com Desconto falha se não for Admin")
    void registerPayment_WithDiscount_ThrowsIfGarcom() {
        mockSecurityRole("ROLE_GARCOM");

        Tab tab = new Tab();
        tab.setTotalAmount(new BigDecimal("105.00"));
        tab.setStatus(TabStatus.OPEN);

        when(tabRepository.findById(1)).thenReturn(Optional.of(tab));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> 
            tabService.registerPayment(1, new BigDecimal("100.00"), new BigDecimal("5.00"))
        );
        assertNotNull(exception);
    }

    @Test
    @DisplayName("Pagamento Agrupado de 2 comandas liquidando tudo")
    void payGroupedTabs_Success() {
        Tab tab1 = new Tab(); tab1.setId(1); tab1.setTotalAmount(new BigDecimal("50.00")); tab1.setStatus(TabStatus.OPEN);
        Tab tab2 = new Tab(); tab2.setId(2); tab2.setTotalAmount(new BigDecimal("30.00")); tab2.setStatus(TabStatus.OPEN);

        when(tabRepository.findAllById(List.of(1, 2))).thenReturn(List.of(tab1, tab2));

        tabService.payGroupedTabs(List.of(1, 2), new BigDecimal("80.00"));

        assertEquals(TabStatus.CLOSED, tab1.getStatus());
        assertEquals(TabStatus.CLOSED, tab2.getStatus());
        assertEquals(new BigDecimal("50.00"), tab1.getPaidAmount());
        assertEquals(new BigDecimal("30.00"), tab2.getPaidAmount());
    }

    @Test
    @DisplayName("Reabrir comanda (Mesa antiga já está ocupada por outra pessoa)")
    void reopenTab_Safely() {
        Table table5 = new Table(); table5.setNumber(5); table5.setOccupied(true);

        Tab oldTab = new Tab(); 
        oldTab.setStatus(TabStatus.CLOSED); 
        oldTab.setTable(table5);
        oldTab.setCustomerName("Cliente Antigo");

        when(tabRepository.findById(1)).thenReturn(Optional.of(oldTab));

        tabService.reopenTab(1);

        assertEquals(TabStatus.OPEN, oldTab.getStatus());
        assertEquals(null, oldTab.getTable()); 
    }
}