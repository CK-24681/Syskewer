package com.syskewer.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.syskewer.api.dto.salon.OrderItemRecordDto;
import com.syskewer.api.dto.salon.OrderRecordDto;
import com.syskewer.api.model.product.Product;
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderItem;
import com.syskewer.api.model.salon.OrderOrigin;
import com.syskewer.api.model.salon.Tab;
import com.syskewer.api.model.salon.TabStatus;
import com.syskewer.api.repository.product.ProductRepository;
import com.syskewer.api.repository.salon.OrderItemRepository;
import com.syskewer.api.repository.salon.OrderRepository;
import com.syskewer.api.repository.salon.TabRepository;
import com.syskewer.api.service.salon.OrderService;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private TabRepository tabRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Deve lançar um pedido, congelar o preço do produto e somar na comanda")
    void placeOrder_Success() {
        // Simula o login do garçom no terminal para que a venda fique registrada no nome dele
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");
        SecurityContextHolder.setContext(securityContext);

        Tab tab = new Tab();
        tab.setId(1);
        tab.setStatus(TabStatus.OPEN);
        tab.setTotalAmount(BigDecimal.ZERO);

        Product product = new Product();
        product.setId(99);
        product.setPrice(new BigDecimal("15.00")); 

        OrderItemRecordDto itemDto = new OrderItemRecordDto(99, 2, false, null, null, null); 
        OrderRecordDto orderDto = new OrderRecordDto(1, OrderOrigin.WAITER, List.of(itemDto));

        when(tabRepository.findById(1)).thenReturn(Optional.of(tab));
        when(productRepository.findById(99)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);
        
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> {
            OrderItem savedItem = (OrderItem) i.getArguments()[0];
            
            // A REGRA DE OURO DA API: Congelamento Anti-Inflação.
            // O sistema é obrigado a buscar o valor no catálogo hoje ("15.00") e travar na venda. 
            // Se o espetinho amanhã subir para 20, o histórico desse cliente se mantém intocável.
            assertEquals(new BigDecimal("15.00"), savedItem.getSoldPrice());
            return savedItem;
        });

        // O garçom aperta o botão de "Confirmar Pedido" no tablet
        orderService.placeOrder(orderDto);

        // A comanda deve somar automaticamente os valores (2 espetos * 15 = 30)
        assertEquals(new BigDecimal("30.00"), tab.getTotalAmount());
        verify(tabRepository, times(1)).save(tab);
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve impedir lançamento de pedidos em comandas fechadas")
    void placeOrder_ThrowsException_WhenTabIsClosed() {
        // O cliente já pagou a conta e a mesa foi encerrada
        Tab tab = new Tab();
        tab.setId(1);
        tab.setStatus(TabStatus.CLOSED); 

        OrderRecordDto orderDto = new OrderRecordDto(1, OrderOrigin.WAITER, List.of());

        when(tabRepository.findById(1)).thenReturn(Optional.of(tab));

        // Trava Final do Caixa: Tentar enviar um novo espetinho para uma conta 
        // já liquidada deve disparar um alarme vermelho imediato e impedir o disparo para a cozinha.
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(orderDto);
        });

        assertTrue(exception.getMessage().contains("fechada"));
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(productRepository);
    }
}