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
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderDetail;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.model.salon.Bill;
import com.syskewer.api.model.salon.BillStatus;
import com.syskewer.api.repository.salon.OrderDetailRepository;
import com.syskewer.api.repository.salon.BillRepository;
import com.syskewer.api.service.salon.OrderService;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderDetailRepository orderDetailRepository;
    @Mock private BillRepository billRepository; 

    @InjectMocks private OrderService orderService;

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

        Bill bill = new Bill();
        bill.setTotalAmount(new BigDecimal("100.00")); 
        bill.setStatus(BillStatus.OPEN);

        Order order = new Order();
        order.setPrepStatus(PrepStatus.QUEUED);
        order.setBill(bill);

        OrderDetail item = new OrderDetail();
        item.setId(1L);
        item.setOrder(order);
        item.setQuantity(10);
        item.setSoldPrice(new BigDecimal("10.00"));

        when(orderDetailRepository.findById(1L)).thenReturn(Optional.of(item));

        orderService.reduceItemQuantity(1L, 2);

        assertEquals(new BigDecimal("80.00"), bill.getTotalAmount());
        assertEquals(8, item.getQuantity());
    }

    @Test
    @DisplayName("Admin pode cancelar um item mesmo com a mesa fechada (Estorno)")
    void cancelOrderItem_ByAdmin_WhenTabClosed() {
        mockSecurityRole("ROLE_ADMINISTRADOR");

        Bill bill = new Bill();
        bill.setStatus(BillStatus.CLOSED);
        bill.setTotalAmount(new BigDecimal("100.00"));
        bill.setPaidAmount(new BigDecimal("100.00"));

        Order order = new Order();
        order.setPrepStatus(PrepStatus.DELIVERED);
        order.setBill(bill);

        OrderDetail item = new OrderDetail();
        item.setOrder(order);
        item.setQuantity(1);
        item.setSoldPrice(new BigDecimal("15.00")); 

        when(orderDetailRepository.findById(1L)).thenReturn(Optional.of(item));

        orderService.cancelOrderItem(1L);

        assertEquals(new BigDecimal("85.00"), bill.getTotalAmount());
        assertEquals(new BigDecimal("85.00"), bill.getPaidAmount());
    }

    @Test
    @DisplayName("Garçom tenta cancelar item que já está na chapa e é bloqueado")
    void cancelOrderItem_ThrowsException_WhenGarcomCancelsPreppingItem() {
        mockSecurityRole("ROLE_GARCOM");

        Order order = new Order();
        order.setPrepStatus(PrepStatus.PREPARING); 
        OrderDetail item = new OrderDetail();
        item.setOrder(order);

        when(orderDetailRepository.findById(1L)).thenReturn(Optional.of(item));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> orderService.cancelOrderItem(1L));
        assertNotNull(exception);
    }
}
