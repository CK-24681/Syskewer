package com.syskewer.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syskewer.api.dto.salon.KitchenOrderDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.product.Menu;
import com.syskewer.api.model.product.PrepLocation;
import com.syskewer.api.model.salon.Bill;
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderDetail;
import com.syskewer.api.model.salon.OrderOrigin;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.model.salon.Table;
import com.syskewer.api.repository.salon.OrderDetailRepository;
import com.syskewer.api.repository.salon.OrderRepository;
import com.syskewer.api.service.salon.KitchenService;

/** Testes da fila de preparo (cozinha / churrasqueira) e da progressão de status. */
@Tag("business-rules")
@ExtendWith(MockitoExtension.class)
class KitchenServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderDetailRepository orderDetailRepository;

    @InjectMocks private KitchenService kitchenService;

    private Order order(long id, PrepStatus status) {
        Bill b = new Bill();
        b.setId(1);
        b.setCustomerName("Balcão X");
        Order o = new Order();
        o.setId(id);
        o.setBill(b);
        o.setPrepStatus(status);
        o.setOrigin(OrderOrigin.WAITER);
        o.setCreatedAt(LocalDateTime.now());
        return o;
    }

    private OrderDetail detail(String menuName, String location) {
        Menu m = new Menu();
        m.setName(menuName);
        if (location != null) {
            PrepLocation p = new PrepLocation();
            p.setName(location);
            m.setPrepLocation(p);
        }
        OrderDetail d = new OrderDetail();
        d.setMenu(m);
        d.setQuantity(1);
        d.setIsToGo(false);
        return d;
    }

    @Test
    @DisplayName("Avançar status QUEUED -> PREPARING persiste o novo status")
    void advance_forward_ok() {
        Order o = order(1L, PrepStatus.QUEUED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

        kitchenService.advanceOrderStatus(1L, PrepStatus.PREPARING);

        assertEquals(PrepStatus.PREPARING, o.getPrepStatus());
        verify(orderRepository).save(o);
    }

    @Test
    @DisplayName("Retroceder status (DELIVERED -> QUEUED) é bloqueado")
    void advance_backward_throws() {
        Order o = order(1L, PrepStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

        assertThrows(BusinessRuleException.class, () -> kitchenService.advanceOrderStatus(1L, PrepStatus.QUEUED));
        assertEquals(PrepStatus.DELIVERED, o.getPrepStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Status nulo ou pedido inexistente é rejeitado")
    void advance_nullOrMissing() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, PrepStatus.QUEUED)));
        when(orderRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> kitchenService.advanceOrderStatus(1L, null));
        assertThrows(ResourceNotFoundException.class, () -> kitchenService.advanceOrderStatus(2L, PrepStatus.READY));
    }

    @Test
    @DisplayName("Fila filtra por praça de preparo e esconde pedidos sem itens da praça")
    void queue_filtersByLocation() {
        Order o1 = order(1L, PrepStatus.QUEUED);
        Order o2 = order(2L, PrepStatus.PREPARING);
        when(orderRepository.findByPrepStatusInOrderByCreatedAtAsc(any())).thenReturn(List.of(o1, o2));
        when(orderDetailRepository.findByOrderId(1L)).thenReturn(List.of(detail("Picanha", "Churrasqueira")));
        when(orderDetailRepository.findByOrderId(2L)).thenReturn(List.of(detail("Salada", "Cozinha")));

        List<KitchenOrderDto> grill = kitchenService.getKitchenQueue("churrasqueira");

        assertEquals(1, grill.size());
        assertEquals(1L, grill.get(0).orderId());
    }

    @Test
    @DisplayName("Fila sem filtro mostra todos; destino de balcão usa 'Tab <id>' e garçom nulo vira Autoatendimento")
    void queue_noFilter_destinationAndWaiter() {
        Order o1 = order(1L, PrepStatus.QUEUED);
        when(orderRepository.findByPrepStatusInOrderByCreatedAtAsc(any())).thenReturn(List.of(o1));
        when(orderDetailRepository.findByOrderId(1L)).thenReturn(List.of(detail("Suco", null)));

        List<KitchenOrderDto> q = kitchenService.getKitchenQueue(null);

        assertEquals(1, q.size());
        assertEquals("Tab 1 (Balcão X)", q.get(0).destination());
        assertEquals("Autoatendimento/Delivery", q.get(0).waiterName());
    }

    @Test
    @DisplayName("Fila de pedido em mesa exibe o número da mesa como destino")
    void queue_tableDestination() {
        Order o1 = order(1L, PrepStatus.QUEUED);
        o1.getBill().getTables().add(new Table(12, true));
        when(orderRepository.findByPrepStatusInOrderByCreatedAtAsc(any())).thenReturn(List.of(o1));
        when(orderDetailRepository.findByOrderId(1L)).thenReturn(List.of(detail("Suco", null)));

        assertEquals("Mesa 12", kitchenService.getKitchenQueue("").get(0).destination());
    }
}
