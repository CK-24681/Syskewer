package com.syskewer.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.syskewer.api.dto.salon.OrderDetailRecordDto;
import com.syskewer.api.dto.salon.OrderRecordDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.product.Menu;
import com.syskewer.api.model.product.Product;
import com.syskewer.api.model.salon.Bill;
import com.syskewer.api.model.salon.BillStatus;
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderDetail;
import com.syskewer.api.model.salon.OrderOrigin;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.repository.product.MenuRepository;
import com.syskewer.api.repository.salon.BillRepository;
import com.syskewer.api.repository.salon.OrderDetailRepository;
import com.syskewer.api.repository.salon.OrderRepository;
import com.syskewer.api.service.salon.OrderService;
import com.syskewer.api.service.salon.StoreSettingsService;

/**
 * Testes complementares da {@link OrderService}: lançamento de pedidos,
 * congelamento de preço, estoque, cancelamento, redução e racha de itens.
 */
@Tag("business-rules")
@ExtendWith(MockitoExtension.class)
class OrderServiceExtendedTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderDetailRepository orderDetailRepository;
    @Mock private BillRepository billRepository;
    @Mock private MenuRepository menuRepository;
    @Mock private StoreSettingsService storeSettingsService;

    @InjectMocks private OrderService orderService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuth(String role, boolean stubPrincipal) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        if (stubPrincipal) {
            when(auth.getPrincipal()).thenReturn("anonymous-test-principal");
        } else {
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
            org.mockito.Mockito.<java.util.Collection<? extends GrantedAuthority>>when(auth.getAuthorities())
                    .thenReturn(authorities);
        }
        SecurityContextHolder.setContext(ctx);
    }

    private Bill bill(int id, String total) {
        Bill b = new Bill();
        b.setId(id);
        b.setCustomerName("Cliente " + id);
        b.setTotalAmount(new BigDecimal(total));
        b.setStatus(BillStatus.OPEN);
        return b;
    }

    private Menu menu(int id, String name, String price, Boolean active) {
        Menu m = new Menu();
        m.setId(id);
        m.setName(name);
        m.setPrice(new BigDecimal(price));
        m.setActive(active);
        return m;
    }

    private OrderRecordDto orderOf(int billId, int menuId, int qty) {
        return new OrderRecordDto(billId, OrderOrigin.WAITER,
                List.of(new OrderDetailRecordDto(menuId, qty, false, null, null, null)));
    }

    private OrderDetail item(String price, int qty, PrepStatus status, Bill bill) {
        Order o = new Order();
        o.setPrepStatus(status);
        o.setBill(bill);
        OrderDetail d = new OrderDetail();
        d.setOrder(o);
        d.setQuantity(qty);
        d.setSoldPrice(new BigDecimal(price));
        return d;
    }

    // ---------- placeOrder ----------

    @Test
    @DisplayName("Lançar pedido com loja fechada é bloqueado")
    void placeOrder_storeClosed_throws() {
        when(storeSettingsService.isStoreOpen()).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> orderService.placeOrder(orderOf(1, 1, 1)));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lançar pedido em conta inexistente retorna 404 de negócio")
    void placeOrder_billNotFound() {
        when(storeSettingsService.isStoreOpen()).thenReturn(true);
        when(billRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.placeOrder(orderOf(1, 1, 1)));
    }

    @Test
    @DisplayName("Lançar pedido em conta fechada ou em fiado é bloqueado")
    void placeOrder_closedOrInDebt_throws() {
        when(storeSettingsService.isStoreOpen()).thenReturn(true);
        Bill closed = bill(1, "0.00");
        closed.setStatus(BillStatus.CLOSED);
        Bill inDebt = bill(2, "0.00");
        inDebt.setStatus(BillStatus.IN_DEBT);
        when(billRepository.findById(1)).thenReturn(Optional.of(closed));
        when(billRepository.findById(2)).thenReturn(Optional.of(inDebt));

        assertThrows(BusinessRuleException.class, () -> orderService.placeOrder(orderOf(1, 1, 1)));
        assertThrows(BusinessRuleException.class, () -> orderService.placeOrder(orderOf(2, 1, 1)));
    }

    @Test
    @DisplayName("Pedido congela o preço (soldPrice) e soma o total na conta")
    void placeOrder_freezesPriceAndUpdatesTotal() {
        mockAuth(null, true);
        when(storeSettingsService.isStoreOpen()).thenReturn(true);
        Bill b = bill(1, "10.00");
        Menu m = menu(5, "Picanha", "12.50", true);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);
        when(menuRepository.findById(5)).thenReturn(Optional.of(m));

        orderService.placeOrder(orderOf(1, 5, 3));

        ArgumentCaptor<OrderDetail> captor = ArgumentCaptor.forClass(OrderDetail.class);
        verify(orderDetailRepository).save(captor.capture());
        // o preço do cardápio muda depois do pedido; o item deve manter o valor original
        m.setPrice(new BigDecimal("99.99"));
        assertEquals(new BigDecimal("12.50"), captor.getValue().getSoldPrice());
        assertEquals(3, captor.getValue().getQuantity());
        assertEquals(new BigDecimal("47.50"), b.getTotalAmount());
    }

    @Test
    @DisplayName("Pedido novo nasce na fila (QUEUED) com a origem informada")
    void placeOrder_newOrderIsQueued() {
        mockAuth(null, true);
        when(storeSettingsService.isStoreOpen()).thenReturn(true);
        when(billRepository.findById(1)).thenReturn(Optional.of(bill(1, "0.00")));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);
        when(menuRepository.findById(5)).thenReturn(Optional.of(menu(5, "Água", "3.00", true)));

        orderService.placeOrder(new OrderRecordDto(1, OrderOrigin.DELIVERY,
                List.of(new OrderDetailRecordDto(5, 1, null, null, null, List.of("gelo")))));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertEquals(PrepStatus.QUEUED, captor.getValue().getPrepStatus());
        assertEquals(OrderOrigin.DELIVERY, captor.getValue().getOrigin());
    }

    @Test
    @DisplayName("Item de cardápio inativo ou inexistente é bloqueado")
    void placeOrder_inactiveOrMissingMenu_throws() {
        mockAuth(null, true);
        when(storeSettingsService.isStoreOpen()).thenReturn(true);
        when(billRepository.findById(1)).thenReturn(Optional.of(bill(1, "0.00")));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);
        when(menuRepository.findById(5)).thenReturn(Optional.of(menu(5, "Fora", "3.00", false)));
        when(menuRepository.findById(6)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> orderService.placeOrder(orderOf(1, 5, 1)));
        assertThrows(ResourceNotFoundException.class, () -> orderService.placeOrder(orderOf(1, 6, 1)));
    }

    @Test
    @DisplayName("Item cujo ingrediente está sem estoque é bloqueado")
    void placeOrder_outOfStock_throws() {
        mockAuth(null, true);
        when(storeSettingsService.isStoreOpen()).thenReturn(true);
        Bill b = bill(1, "0.00");
        Menu m = menu(5, "Chopp", "9.00", true);
        m.setProducts(List.of(new Product("Barril", true, false)));
        when(billRepository.findById(1)).thenReturn(Optional.of(b));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);
        when(menuRepository.findById(5)).thenReturn(Optional.of(m));

        assertThrows(BusinessRuleException.class, () -> orderService.placeOrder(orderOf(1, 5, 1)));
        assertEquals(new BigDecimal("0.00"), b.getTotalAmount());
    }

    // ---------- cancelOrderItem ----------

    @Test
    @DisplayName("Garçom cancela item na fila (QUEUED) e o total da conta é abatido")
    void cancelOrderItem_garcom_queued_ok() {
        mockAuth("ROLE_GARCOM", false);
        Bill b = bill(1, "50.00");
        OrderDetail d = item("10.00", 2, PrepStatus.QUEUED, b);
        when(orderDetailRepository.findById(1L)).thenReturn(Optional.of(d));

        orderService.cancelOrderItem(1L);

        assertEquals(new BigDecimal("30.00"), b.getTotalAmount());
        verify(orderDetailRepository).delete(d);
    }

    @Test
    @DisplayName("Garçom não estorna item de conta fechada")
    void cancelOrderItem_garcom_closedBill_throws() {
        mockAuth("ROLE_GARCOM", false);
        Bill b = bill(1, "50.00");
        b.setStatus(BillStatus.CLOSED);
        OrderDetail d = item("10.00", 2, PrepStatus.QUEUED, b);
        when(orderDetailRepository.findById(1L)).thenReturn(Optional.of(d));

        assertThrows(BusinessRuleException.class, () -> orderService.cancelOrderItem(1L));
        verify(orderDetailRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Cancelar item inexistente retorna 404 de negócio")
    void cancelOrderItem_notFound() {
        when(orderDetailRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.cancelOrderItem(9L));
    }

    // ---------- reduceItemQuantity ----------

    @Test
    @DisplayName("Reduzir quantidade com valor nulo, zero, negativo ou >= quantidade é bloqueado")
    void reduceItemQuantity_invalidQuantities() {
        mockAuth("ROLE_ADMINISTRADOR", false);
        OrderDetail d = item("10.00", 3, PrepStatus.QUEUED, bill(1, "30.00"));
        when(orderDetailRepository.findById(1L)).thenReturn(Optional.of(d));

        assertThrows(BusinessRuleException.class, () -> orderService.reduceItemQuantity(1L, null));
        assertThrows(BusinessRuleException.class, () -> orderService.reduceItemQuantity(1L, 0));
        assertThrows(BusinessRuleException.class, () -> orderService.reduceItemQuantity(1L, -1));
        assertThrows(BusinessRuleException.class, () -> orderService.reduceItemQuantity(1L, 3));
        assertEquals(3, d.getQuantity());
    }

    @Test
    @DisplayName("Garçom não reduz quantidade de item já em preparo; admin pode")
    void reduceItemQuantity_inPrep_adminOnly() {
        Bill b = bill(1, "30.00");
        OrderDetail d = item("10.00", 3, PrepStatus.PREPARING, b);
        when(orderDetailRepository.findById(1L)).thenReturn(Optional.of(d));

        mockAuth("ROLE_GARCOM", false);
        assertThrows(BusinessRuleException.class, () -> orderService.reduceItemQuantity(1L, 1));

        mockAuth("ROLE_ADMINISTRADOR", false);
        orderService.reduceItemQuantity(1L, 1);
        assertEquals(2, d.getQuantity());
        assertEquals(new BigDecimal("20.00"), b.getTotalAmount());
    }

    @Test
    @DisplayName("Reduzir item de conta fechada estorna também o valor pago")
    void reduceItemQuantity_closedBill_refundsPaid() {
        mockAuth("ROLE_ADMINISTRADOR", false);
        Bill b = bill(1, "30.00");
        b.setStatus(BillStatus.CLOSED);
        b.setPaidAmount(new BigDecimal("30.00"));
        OrderDetail d = item("10.00", 3, PrepStatus.DELIVERED, b);
        when(orderDetailRepository.findById(1L)).thenReturn(Optional.of(d));

        orderService.reduceItemQuantity(1L, 1);

        assertEquals(new BigDecimal("20.00"), b.getTotalAmount());
        assertEquals(new BigDecimal("20.00"), b.getPaidAmount());
    }

    // ---------- splitItem ----------

    private OrderDetail splittable(Bill original, String price, int qty) {
        OrderDetail d = item(price, qty, PrepStatus.DELIVERED, original);
        d.setMenu(menu(1, "Porção", price, true));
        return d;
    }

    @Test
    @DisplayName("Racha de item de R$ 30,00 entre 3 pessoas: R$ 10,00 para cada, total preservado")
    void splitItem_evenSplit() {
        Bill original = bill(1, "30.00");
        Bill t1 = bill(2, "0.00");
        Bill t2 = bill(3, "0.00");
        OrderDetail d = splittable(original, "30.00", 1);
        when(orderDetailRepository.findById(1L)).thenReturn(Optional.of(d));
        when(billRepository.findById(2)).thenReturn(Optional.of(t1));
        when(billRepository.findById(3)).thenReturn(Optional.of(t2));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        orderService.splitItem(1L, List.of(2, 3));

        assertEquals(new BigDecimal("10.00"), original.getTotalAmount());
        assertEquals(new BigDecimal("10.00"), t1.getTotalAmount());
        assertEquals(new BigDecimal("10.00"), t2.getTotalAmount());
    }

    @Test
    @DisplayName("Racha com resto: R$ 10,00 entre 3 pessoas preserva o total geral (invariante)")
    void splitItem_unevenSplit_preservesGrandTotal() {
        Bill original = bill(1, "10.00");
        Bill t1 = bill(2, "0.00");
        Bill t2 = bill(3, "0.00");
        OrderDetail d = splittable(original, "10.00", 1);
        when(orderDetailRepository.findById(1L)).thenReturn(Optional.of(d));
        when(billRepository.findById(2)).thenReturn(Optional.of(t1));
        when(billRepository.findById(3)).thenReturn(Optional.of(t2));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        orderService.splitItem(1L, List.of(2, 3));

        assertEquals(new BigDecimal("3.33"), t1.getTotalAmount());
        assertEquals(new BigDecimal("3.33"), t2.getTotalAmount());
        BigDecimal grand = original.getTotalAmount().add(t1.getTotalAmount()).add(t2.getTotalAmount());
        assertEquals(0, new BigDecimal("10.00").compareTo(grand));
    }

    @Test
    @DisplayName("Racha sem contas de destino ou para conta fechada é bloqueado")
    void splitItem_invalidTargets() {
        Bill original = bill(1, "30.00");
        Bill closed = bill(2, "0.00");
        closed.setStatus(BillStatus.CLOSED);
        OrderDetail d = splittable(original, "30.00", 1);
        when(orderDetailRepository.findById(1L)).thenReturn(Optional.of(d));
        when(billRepository.findById(2)).thenReturn(Optional.of(closed));
        when(billRepository.findById(404)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        assertThrows(BusinessRuleException.class, () -> orderService.splitItem(1L, List.of()));
        assertThrows(BusinessRuleException.class, () -> orderService.splitItem(1L, List.of(2)));
        assertThrows(BusinessRuleException.class, () -> orderService.splitItem(1L, List.of(404)));
    }

    @Test
    @DisplayName("Racha de item inexistente retorna 404 de negócio")
    void splitItem_itemNotFound() {
        when(orderDetailRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.splitItem(9L, List.of(1)));
    }
}
