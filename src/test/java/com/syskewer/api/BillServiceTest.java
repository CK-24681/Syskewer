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

import com.syskewer.api.dto.salon.BillOpenDto;
import com.syskewer.api.dto.salon.BillSummaryDto;
import com.syskewer.api.dto.salon.BillUpdateDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.model.salon.ConsumptionType;
import com.syskewer.api.model.salon.Bill;
import com.syskewer.api.model.salon.BillStatus;
import com.syskewer.api.model.salon.Table;
import com.syskewer.api.repository.salon.BillRepository;
import com.syskewer.api.repository.salon.TableRepository;
import com.syskewer.api.service.salon.StoreSettingsService;
import com.syskewer.api.service.salon.BillService;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock private BillRepository billRepository;
    @Mock private TableRepository tableRepository;
    @Mock private StoreSettingsService storeSettingsService;

    @InjectMocks private BillService billService;

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
        BillOpenDto dto = new BillOpenDto("", 4, ConsumptionType.MESA);
        
        Table table = new Table(); table.setId(4); table.setNumber(4); table.setOccupied(false);
        when(tableRepository.findByNumber(4)).thenReturn(Optional.of(table));
        
        when(billRepository.findByTableIdAndStatus(any(), any())).thenReturn(List.of(new Bill()));
        
        when(billRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        BillSummaryDto result = billService.openTab(dto);

        assertEquals("Mesa 4 - Cliente 2", result.customerName());
    }

    @Test
    @DisplayName("Pagamento com Desconto falha se não for Admin")
    void registerPayment_WithDiscount_ThrowsIfGarcom() {
        mockSecurityRole("ROLE_GARCOM");

        Bill bill = new Bill();
        bill.setTotalAmount(new BigDecimal("105.00"));
        bill.setStatus(BillStatus.OPEN);

        when(billRepository.findById(1)).thenReturn(Optional.of(bill));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> 
            billService.registerPayment(1, new BigDecimal("100.00"), new BigDecimal("5.00"))
        );
        assertNotNull(exception);
    }

    @Test
    @DisplayName("Pagamento Agrupado de 2 contas liquidando tudo")
    void payGroupedTabs_Success() {
        Bill bill1 = new Bill(); bill1.setId(1); bill1.setTotalAmount(new BigDecimal("50.00")); bill1.setStatus(BillStatus.OPEN);
        Bill bill2 = new Bill(); bill2.setId(2); bill2.setTotalAmount(new BigDecimal("30.00")); bill2.setStatus(BillStatus.OPEN);

        when(billRepository.findAllById(List.of(1, 2))).thenReturn(List.of(bill1, bill2));

        billService.payGroupedTabs(List.of(1, 2), new BigDecimal("80.00"));

        assertEquals(BillStatus.CLOSED, bill1.getStatus());
        assertEquals(BillStatus.CLOSED, bill2.getStatus());
        assertEquals(new BigDecimal("50.00"), bill1.getPaidAmount());
        assertEquals(new BigDecimal("30.00"), bill2.getPaidAmount());
    }

    @Test
    @DisplayName("Reabrir conta (Mesa antiga já está ocupada por outra pessoa)")
    void reopenTab_Safely() {
        Table table5 = new Table(); table5.setNumber(5); table5.setOccupied(true);

        Bill oldTab = new Bill(); 
        oldTab.setStatus(BillStatus.CLOSED); 
        oldTab.getTables().add(table5);
        oldTab.setCustomerName("Cliente Antigo");

        when(billRepository.findById(1)).thenReturn(Optional.of(oldTab));

        billService.reopenTab(1);

        assertEquals(BillStatus.OPEN, oldTab.getStatus());
        assertEquals(0, oldTab.getTables().size()); 
    }

    @Test
    @DisplayName("Atualizar nome do cliente da conta com patchTab")
    void patchTab_UpdatesCustomerName() {
        Bill bill = new Bill();
        bill.setId(1);
        bill.setCustomerName("Cliente Original");
        bill.setConsumptionType(ConsumptionType.MESA);

        when(billRepository.findById(1)).thenReturn(Optional.of(bill));
        when(billRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        BillUpdateDto dto = new BillUpdateDto("Cliente Atualizado");
        BillSummaryDto result = billService.patchTab(1, dto);

        assertEquals("Cliente Atualizado", result.customerName());
        assertEquals("Cliente Atualizado", bill.getCustomerName());
    }

    @Test
    @DisplayName("Pagamento com desconto maior que o saldo falha")
    void registerPayment_DiscountGreaterThanBalance_Throws() {
        mockSecurityRole("ROLE_ADMINISTRADOR");
        Bill bill = new Bill();
        bill.setTotalAmount(new BigDecimal("50.00"));
        bill.setPaidAmount(new BigDecimal("10.00")); 
        bill.setStatus(BillStatus.OPEN);

        when(billRepository.findById(1)).thenReturn(Optional.of(bill));

        assertThrows(BusinessRuleException.class, () -> 
            billService.registerPayment(1, new BigDecimal("10.00"), new BigDecimal("45.00"))
        );
    }

    @Test
    @DisplayName("Arquivar conta sem saldo devedor falha")
    void archiveAsCredit_NoBalance_Throws() {
        Bill bill = new Bill();
        bill.setTotalAmount(new BigDecimal("50.00"));
        bill.setPaidAmount(new BigDecimal("50.00"));
        bill.setStatus(BillStatus.OPEN);

        when(billRepository.findById(1)).thenReturn(Optional.of(bill));

        assertThrows(BusinessRuleException.class, () -> 
            billService.archiveAsCredit(1, "123456789")
        );
    }

    @Test
    @DisplayName("Cancelar conta fechada falha")
    void cancelTab_ClosedTab_Throws() {
        Bill bill = new Bill();
        bill.setStatus(BillStatus.CLOSED);

        when(billRepository.findById(1)).thenReturn(Optional.of(bill));

        assertThrows(BusinessRuleException.class, () -> 
            billService.cancelTab(1)
        );
    }

    @Test
    @DisplayName("Transferir conta fechada falha")
    void transferTab_ClosedTab_Throws() {
        Bill bill = new Bill();
        bill.setStatus(BillStatus.CLOSED);

        when(billRepository.findById(1)).thenReturn(Optional.of(bill));

        assertThrows(BusinessRuleException.class, () -> 
            billService.transferTab(1, 2)
        );
    }

    @Test
    @DisplayName("Pagamento agrupado com valor zerado/negativo falha")
    void payGroupedTabs_ZeroAmount_Throws() {
        assertThrows(BusinessRuleException.class, () -> 
            billService.payGroupedTabs(List.of(1, 2), BigDecimal.ZERO)
        );
    }
}
