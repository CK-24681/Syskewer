package com.syskewer.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.model.salon.ComandaItem;
import com.syskewer.api.model.salon.ComandaItemDetail;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.model.salon.Comanda;
import com.syskewer.api.model.salon.ComandaStatus;
import com.syskewer.api.repository.salon.ComandaItemDetailRepository;
import com.syskewer.api.repository.salon.ComandaRepository;
import com.syskewer.api.service.salon.ComandaItemService;

@ExtendWith(MockitoExtension.class)
class ComandaItemServiceTest {

    @Mock private ComandaItemDetailRepository comandaItemDetailRepository;
    @Mock private ComandaRepository comandaRepository; 

    @InjectMocks private ComandaItemService comandaItemService;

    private void mockSecurityRole(String role) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        org.mockito.Mockito.<java.util.Collection<? extends GrantedAuthority>>when(auth.getAuthorities()).thenReturn(authorities);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    @DisplayName("Garçom reduz a quantidade e abate do valor da mesa")
    void reduceItemQuantity_Success() {
        mockSecurityRole("ROLE_GARCOM");

        Comanda comanda = new Comanda();
        comanda.setTotalAmount(new BigDecimal("100.00")); 
        comanda.setStatus(ComandaStatus.OPEN);

        ComandaItem comandaItem = new ComandaItem();
        comandaItem.setPrepStatus(PrepStatus.QUEUED);
        comandaItem.setComanda(comanda);

        ComandaItemDetail item = new ComandaItemDetail();
        item.setId(1L);
        item.setComandaItem(comandaItem);
        item.setQuantity(10);
        item.setSoldPrice(new BigDecimal("10.00"));

        when(comandaItemDetailRepository.findById(1L)).thenReturn(Optional.of(item));

        comandaItemService.reduceItemQuantity(1L, 2);

        assertEquals(new BigDecimal("80.00"), comanda.getTotalAmount());
        assertEquals(8, item.getQuantity());
    }

    @Test
    @DisplayName("Admin pode cancelar um item mesmo com a mesa fechada (Estorno)")
    void cancelOrderItem_ByAdmin_WhenTabClosed() {
        mockSecurityRole("ROLE_ADMINISTRADOR");

        Comanda comanda = new Comanda();
        comanda.setStatus(ComandaStatus.CLOSED);
        comanda.setTotalAmount(new BigDecimal("100.00"));
        comanda.setPaidAmount(new BigDecimal("100.00"));

        ComandaItem comandaItem = new ComandaItem();
        comandaItem.setPrepStatus(PrepStatus.DELIVERED);
        comandaItem.setComanda(comanda);

        ComandaItemDetail item = new ComandaItemDetail();
        item.setComandaItem(comandaItem);
        item.setQuantity(1);
        item.setSoldPrice(new BigDecimal("15.00")); 

        when(comandaItemDetailRepository.findById(1L)).thenReturn(Optional.of(item));

        comandaItemService.cancelOrderItem(1L);

        assertEquals(new BigDecimal("85.00"), comanda.getTotalAmount());
        assertEquals(new BigDecimal("85.00"), comanda.getPaidAmount());
    }

    @Test
    @DisplayName("Garçom tenta cancelar item que já está na chapa e é bloqueado")
    void cancelOrderItem_ThrowsException_WhenGarcomCancelsPreppingItem() {
        mockSecurityRole("ROLE_GARCOM");

        ComandaItem comandaItem = new ComandaItem();
        comandaItem.setPrepStatus(PrepStatus.PREPARING); 
        ComandaItemDetail item = new ComandaItemDetail();
        item.setComandaItem(comandaItem);

        when(comandaItemDetailRepository.findById(1L)).thenReturn(Optional.of(item));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> comandaItemService.cancelOrderItem(1L));
        assertNotNull(exception);
    }
}
