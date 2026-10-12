package com.syskewer.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.syskewer.api.dto.salon.BillDeliveryDto;
import com.syskewer.api.dto.salon.BillOpenDto;
import com.syskewer.api.dto.salon.BillPartialDto;
import com.syskewer.api.dto.salon.BillSummaryDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.product.Menu;
import com.syskewer.api.model.salon.Bill;
import com.syskewer.api.model.salon.BillStatus;
import com.syskewer.api.model.salon.ConsumptionType;
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderDetail;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.model.salon.Table;
import com.syskewer.api.repository.salon.BillRepository;
import com.syskewer.api.repository.salon.OrderDetailRepository;
import com.syskewer.api.repository.salon.OrderRepository;
import com.syskewer.api.repository.salon.TableRepository;
import com.syskewer.api.service.salon.BillService;
import com.syskewer.api.service.salon.StoreSettingsService;

/**
 * Testes complementares de regras de negócio da {@link BillService}.
 * Criados na atividade "Avaliação de Saída" para elevar a cobertura das regras
 * críticas (fechamento, couvert, fiado, pagamento, transferência, delivery...).
 */
@Tag("business-rules")
@ExtendWith(MockitoExtension.class)
class BillServiceExtendedTest {

    @Mock private BillRepository billRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderDetailRepository orderDetailRepository;
    @Mock private TableRepository tableRepository;
    @Mock private StoreSettingsService storeSettingsService;

    @InjectMocks private BillService billService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ---------- helpers ----------

    private void mockSecurityRole(String role) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        org.mockito.Mockito.<java.util.Collection<? extends GrantedAuthority>>when(auth.getAuthorities())
                .thenReturn(authorities);
        SecurityContextHolder.setContext(ctx);
    }

    private Bill bill(int id, String total, String paid) {
        Bill b = new Bill();
        b.setId(id);
        b.setTotalAmount(new BigDecimal(total));
        b.setPaidAmount(new BigDecimal(paid));
        b.setStatus(BillStatus.OPEN);
        b.setConsumptionType(ConsumptionType.MESA);
        return b;
    }

    private Table table(int id, int number, boolean occupied) {
        Table t = new Table(number, occupied);
        t.setId(id);
        return t;
    }

    // ---------- closeTab ----------

    @Test
    @DisplayName("Fechar conta com saldo devedor é bloqueado")
    void closeTab_withBalance_throws() {
        Bill b = bill(1, "50.00", "20.00");
        when(billRepository.findById(1)).thenReturn(Optional.of(b));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> billService.closeTab(1));

        assertTrue(ex.getMessage().contains("30.00"));
        assertEquals(BillStatus.OPEN, b.getStatus());
        verify(billRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fechar conta quitada libera a mesa quando não há outras contas abertas")
    void closeTab_paid_releasesTable() {
        Table t = table(10, 10, true);
        Bill b = bill(1, "50.00", "50.00");
        b.getTables().add(t);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));
        when(billRepository.existsByTableIdAndStatus(10, BillStatus.OPEN)).thenReturn(false);

        billService.closeTab(1);

        assertEquals(BillStatus.CLOSED, b.getStatus());
        assertFalse(t.getOccupied());
        verify(tableRepository).save(t);
    }

    @Test
    @DisplayName("Fechar conta mantém a mesa ocupada se ainda há outra conta aberta nela")
    void closeTab_keepsTableOccupiedWhenOtherOpenBills() {
        Table t = table(10, 10, true);
        Bill b = bill(1, "0.00", "0.00");
        b.getTables().add(t);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));
        when(billRepository.existsByTableIdAndStatus(10, BillStatus.OPEN)).thenReturn(true);

        billService.closeTab(1);

        assertTrue(t.getOccupied());
        verify(tableRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fechar conta já fechada ou inexistente falha")
    void closeTab_closedOrMissing_throws() {
        Bill b = bill(1, "0.00", "0.00");
        b.setStatus(BillStatus.CLOSED);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));
        when(billRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> billService.closeTab(1));
        assertThrows(ResourceNotFoundException.class, () -> billService.closeTab(99));
    }

    // ---------- couvert ----------

    @Test
    @DisplayName("Couvert em mesa com 2 contas é dividido igualmente (R$ 2,50 cada)")
    void toggleCoverCharge_table_splitsEqually() {
        Table t = table(10, 10, true);
        Bill b1 = bill(1, "10.00", "0.00");
        Bill b2 = bill(2, "20.00", "0.00");
        b1.getTables().add(t);
        b2.getTables().add(t);
        when(billRepository.findById(1)).thenReturn(Optional.of(b1));
        when(billRepository.findByTableIdAndStatus(10, BillStatus.OPEN)).thenReturn(List.of(b1, b2));

        billService.toggleCoverCharge(1);

        assertEquals(new BigDecimal("12.50"), b1.getTotalAmount());
        assertEquals(new BigDecimal("22.50"), b2.getTotalAmount());
        assertTrue(b1.getApplyCoverCharge());
        assertTrue(b2.getApplyCoverCharge());
    }

    @Test
    @DisplayName("Couvert em mesa com 3 contas: cada uma paga R$ 1,67 (arredondamento HALF_UP)")
    void toggleCoverCharge_table_threeBills_rounding() {
        Table t = table(10, 10, true);
        Bill b1 = bill(1, "0.00", "0.00");
        Bill b2 = bill(2, "0.00", "0.00");
        Bill b3 = bill(3, "0.00", "0.00");
        for (Bill b : List.of(b1, b2, b3)) b.getTables().add(t);
        when(billRepository.findById(1)).thenReturn(Optional.of(b1));
        when(billRepository.findByTableIdAndStatus(10, BillStatus.OPEN)).thenReturn(List.of(b1, b2, b3));

        billService.toggleCoverCharge(1);

        assertEquals(new BigDecimal("1.67"), b1.getTotalAmount());
        assertEquals(new BigDecimal("1.67"), b2.getTotalAmount());
        assertEquals(new BigDecimal("1.67"), b3.getTotalAmount());
    }

    @Test
    @DisplayName("Couvert em balcão (sem mesa) é aplicado individualmente (R$ 5,00)")
    void toggleCoverCharge_balcao_individual() {
        Bill b = bill(1, "10.00", "0.00");
        b.setConsumptionType(ConsumptionType.BALCAO);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));

        billService.toggleCoverCharge(1);

        assertEquals(new BigDecimal("15.00"), b.getTotalAmount());
        assertTrue(b.getApplyCoverCharge());
    }

    @Test
    @DisplayName("Aplicar couvert duas vezes na mesma conta/mesa é bloqueado")
    void toggleCoverCharge_twice_throws() {
        Bill b = bill(1, "15.00", "0.00");
        b.setApplyCoverCharge(true);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));

        assertThrows(BusinessRuleException.class, () -> billService.toggleCoverCharge(1));
        assertEquals(new BigDecimal("15.00"), b.getTotalAmount());
    }

    @Test
    @DisplayName("Aplicar couvert em conta fechada é bloqueado")
    void toggleCoverCharge_closedBill_throws() {
        Bill b = bill(1, "10.00", "10.00");
        b.setStatus(BillStatus.CLOSED);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));

        assertThrows(BusinessRuleException.class, () -> billService.toggleCoverCharge(1));
    }

    @Test
    @DisplayName("Remover couvert de mesa com 2 contas subtrai R$ 2,50 da conta alvo")
    void removeCoverCharge_table_twoBills() {
        Table t = table(10, 10, true);
        Bill b1 = bill(1, "12.50", "0.00");
        Bill b2 = bill(2, "22.50", "0.00");
        b1.setApplyCoverCharge(true);
        b2.setApplyCoverCharge(true);
        b1.getTables().add(t);
        b2.getTables().add(t);
        when(billRepository.findById(1)).thenReturn(Optional.of(b1));
        when(billRepository.findByTableIdAndStatus(10, BillStatus.OPEN)).thenReturn(List.of(b1, b2));

        billService.removeCoverCharge(1);

        assertEquals(new BigDecimal("10.00"), b1.getTotalAmount());
        assertFalse(b1.getApplyCoverCharge());
        assertTrue(b2.getApplyCoverCharge());
    }

    @Test
    @DisplayName("Remover couvert de conta fechada falha")
    void removeCoverCharge_closed_throws() {
        Bill b = bill(1, "10.00", "10.00");
        b.setStatus(BillStatus.CLOSED);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));

        assertThrows(BusinessRuleException.class, () -> billService.removeCoverCharge(1));
    }

    // ---------- fiado ----------

    @Test
    @DisplayName("Arquivar como fiado: status IN_DEBT, documento aparado, data registrada e mesa liberada")
    void archiveAsCredit_success() {
        Table t = table(10, 10, true);
        Bill b = bill(1, "50.00", "10.00");
        b.getTables().add(t);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));
        when(billRepository.existsByTableIdAndStatus(10, BillStatus.OPEN)).thenReturn(false);

        billService.archiveAsCredit(1, "  11999990000 ");

        assertEquals(BillStatus.IN_DEBT, b.getStatus());
        assertEquals("11999990000", b.getCustomerDocOrPhone());
        assertTrue(b.getDeferredDate() != null);
        assertFalse(t.getOccupied());
    }

    @Test
    @DisplayName("Arquivar como fiado exige documento/telefone e conta aberta")
    void archiveAsCredit_validations() {
        Bill open = bill(1, "50.00", "0.00");
        Bill closed = bill(2, "50.00", "0.00");
        closed.setStatus(BillStatus.CLOSED);
        when(billRepository.findById(1)).thenReturn(Optional.of(open));
        when(billRepository.findById(2)).thenReturn(Optional.of(closed));

        assertThrows(BusinessRuleException.class, () -> billService.archiveAsCredit(1, "   "));
        assertThrows(BusinessRuleException.class, () -> billService.archiveAsCredit(1, null));
        assertThrows(BusinessRuleException.class, () -> billService.archiveAsCredit(2, "123"));
        assertEquals(BillStatus.OPEN, open.getStatus());
    }

    // ---------- openTab ----------

    @Test
    @DisplayName("Abrir conta com loja fechada é bloqueado")
    void openTab_storeClosed_throws() {
        when(storeSettingsService.isStoreOpen()).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> billService.openTab(new BillOpenDto("João", 1, ConsumptionType.MESA)));
        verify(billRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abrir conta em mesa inexistente falha")
    void openTab_tableNotFound_throws() {
        when(storeSettingsService.isStoreOpen()).thenReturn(true);
        when(tableRepository.findByNumber(77)).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> billService.openTab(new BillOpenDto("João", 77, ConsumptionType.MESA)));
        assertTrue(ex.getMessage().contains("77"));
    }

    @Test
    @DisplayName("Abrir conta em mesa livre marca a mesa como ocupada e mantém o nome informado (aparado)")
    void openTab_freeTable_marksOccupied() {
        when(storeSettingsService.isStoreOpen()).thenReturn(true);
        Table t = table(3, 3, false);
        when(tableRepository.findByNumber(3)).thenReturn(Optional.of(t));
        when(billRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        BillSummaryDto result = billService.openTab(new BillOpenDto("  Maria  ", 3, ConsumptionType.MESA));

        assertEquals("Maria", result.customerName());
        assertEquals(List.of(3), result.tableNumbers());
        assertEquals("MESA", result.consumptionType());
        assertTrue(t.getOccupied());
        verify(tableRepository).save(t);
    }

    @Test
    @DisplayName("Abrir conta de balcão sem nome atribui 'Cliente Balcão'")
    void openTab_balcao_defaultName() {
        when(storeSettingsService.isStoreOpen()).thenReturn(true);
        when(billRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        BillSummaryDto result = billService.openTab(new BillOpenDto(null, 1, ConsumptionType.BALCAO));

        assertEquals("Cliente Balcão", result.customerName());
        assertTrue(result.tableNumbers().isEmpty());
        verify(tableRepository, never()).findByNumber(any());
    }

    // ---------- registerPayment ----------

    @Test
    @DisplayName("Pagamento parcial reduz o saldo e mantém a conta aberta")
    void registerPayment_partial() {
        Bill b = bill(1, "100.00", "0.00");
        when(billRepository.findById(1)).thenReturn(Optional.of(b));

        String msg = billService.registerPayment(1, new BigDecimal("40.00"), null);

        assertEquals(new BigDecimal("40.00"), b.getPaidAmount());
        assertEquals(BillStatus.OPEN, b.getStatus());
        assertTrue(msg.contains("60.00"));
    }

    @Test
    @DisplayName("Pagamento final quita, fecha a conta e libera a mesa")
    void registerPayment_final_closesAndReleasesTable() {
        Table t = table(10, 10, true);
        Bill b = bill(1, "100.00", "40.00");
        b.getTables().add(t);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));
        when(billRepository.existsByTableIdAndStatus(10, BillStatus.OPEN)).thenReturn(false);

        String msg = billService.registerPayment(1, new BigDecimal("60.00"), BigDecimal.ZERO);

        assertEquals(BillStatus.CLOSED, b.getStatus());
        assertFalse(t.getOccupied());
        assertTrue(msg.contains("closed"));
    }

    @Test
    @DisplayName("Pagamento maior que o saldo é bloqueado")
    void registerPayment_overpay_throws() {
        Bill b = bill(1, "100.00", "90.00");
        when(billRepository.findById(1)).thenReturn(Optional.of(b));

        assertThrows(BusinessRuleException.class,
                () -> billService.registerPayment(1, new BigDecimal("10.01"), null));
        assertEquals(new BigDecimal("90.00"), b.getPaidAmount());
    }

    @Test
    @DisplayName("Pagamento em conta já fechada é bloqueado")
    void registerPayment_closed_throws() {
        Bill b = bill(1, "100.00", "100.00");
        b.setStatus(BillStatus.CLOSED);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));

        assertThrows(BusinessRuleException.class,
                () -> billService.registerPayment(1, new BigDecimal("1.00"), null));
    }

    @Test
    @DisplayName("Administrador concede desconto e quita a conta")
    void registerPayment_adminDiscount_closes() {
        mockSecurityRole("ROLE_ADMINISTRADOR");
        Bill b = bill(1, "100.00", "0.00");
        when(billRepository.findById(1)).thenReturn(Optional.of(b));

        billService.registerPayment(1, new BigDecimal("90.00"), new BigDecimal("10.00"));

        assertEquals(new BigDecimal("90.00"), b.getTotalAmount());
        assertEquals(BillStatus.CLOSED, b.getStatus());
    }

    @Test
    @DisplayName("Conta inexistente retorna ResourceNotFoundException")
    void registerPayment_notFound() {
        when(billRepository.findById(5)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> billService.registerPayment(5, BigDecimal.TEN, null));
    }

    // ---------- delivery ----------

    @Test
    @DisplayName("Conta de delivery nasce com taxa de entrega R$ 5,00 já somada ao total")
    void openDeliveryTab_appliesFee() {
        when(billRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        BillSummaryDto r = billService.openDeliveryTab(new BillDeliveryDto("  Ana ", " Rua A, 10 "));

        assertEquals("Ana", r.customerName());
        assertEquals("DELIVERY", r.consumptionType());
        assertEquals(new BigDecimal("5.00"), r.totalAmount());
        assertTrue(r.tableNumbers().isEmpty());
    }

    // ---------- cancelTab ----------

    @Test
    @DisplayName("Cancelar conta vazia exclui e libera a mesa")
    void cancelTab_empty_deletesAndReleasesTable() {
        Table t = table(10, 10, true);
        Bill b = bill(1, "0.00", "0.00");
        b.getTables().add(t);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));
        when(billRepository.existsByTableIdAndStatus(10, BillStatus.OPEN)).thenReturn(false);

        billService.cancelTab(1);

        verify(billRepository).delete(b);
        assertFalse(t.getOccupied());
    }

    @Test
    @DisplayName("Cancelar conta com consumo ou em fiado é bloqueado")
    void cancelTab_withConsumptionOrInDebt_throws() {
        Bill withConsumption = bill(1, "10.00", "0.00");
        Bill inDebt = bill(2, "0.00", "0.00");
        inDebt.setStatus(BillStatus.IN_DEBT);
        when(billRepository.findById(1)).thenReturn(Optional.of(withConsumption));
        when(billRepository.findById(2)).thenReturn(Optional.of(inDebt));

        assertThrows(BusinessRuleException.class, () -> billService.cancelTab(1));
        assertThrows(BusinessRuleException.class, () -> billService.cancelTab(2));
        verify(billRepository, never()).delete(any());
    }

    // ---------- transferTab ----------

    @Test
    @DisplayName("Transferir conta para mesa livre: ocupa destino e libera origem")
    void transferTab_success() {
        Table origin = table(1, 1, true);
        Table dest = table(2, 2, false);
        Bill b = bill(1, "30.00", "0.00");
        b.getTables().add(origin);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));
        when(tableRepository.findByNumber(2)).thenReturn(Optional.of(dest));
        when(billRepository.existsByTableIdAndStatus(1, BillStatus.OPEN)).thenReturn(false);

        String msg = billService.transferTab(1, 2);

        assertTrue(dest.getOccupied());
        assertFalse(origin.getOccupied());
        assertEquals(List.of(dest), b.getTables());
        assertTrue(msg.contains("2"));
    }

    @Test
    @DisplayName("Transferir para mesa ocupada ou inexistente é bloqueado")
    void transferTab_destinationInvalid_throws() {
        Bill b = bill(1, "30.00", "0.00");
        when(billRepository.findById(1)).thenReturn(Optional.of(b));
        when(tableRepository.findByNumber(2)).thenReturn(Optional.of(table(2, 2, true)));
        when(tableRepository.findByNumber(3)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> billService.transferTab(1, 2));
        assertThrows(ResourceNotFoundException.class, () -> billService.transferTab(1, 3));
    }

    // ---------- payGroupedTabs ----------

    @Test
    @DisplayName("Pagamento agrupado com valor maior calcula o troco")
    void payGroupedTabs_change() {
        Bill b1 = bill(1, "50.00", "0.00");
        Bill b2 = bill(2, "30.00", "0.00");
        when(billRepository.findAllById(List.of(1, 2))).thenReturn(List.of(b1, b2));

        String msg = billService.payGroupedTabs(List.of(1, 2), new BigDecimal("100.00"));

        assertTrue(msg.contains("Change: R$ 20.00"));
        assertEquals(BillStatus.CLOSED, b1.getStatus());
        assertEquals(BillStatus.CLOSED, b2.getStatus());
    }

    @Test
    @DisplayName("Pagamento agrupado insuficiente, vazio ou com conta fechada é bloqueado")
    void payGroupedTabs_invalidCases() {
        Bill b1 = bill(1, "50.00", "0.00");
        Bill closed = bill(2, "30.00", "30.00");
        closed.setStatus(BillStatus.CLOSED);
        when(billRepository.findAllById(List.of(1))).thenReturn(List.of(b1));
        when(billRepository.findAllById(List.of(2))).thenReturn(List.of(closed));
        when(billRepository.findAllById(List.of(9))).thenReturn(List.of());

        assertThrows(BusinessRuleException.class, () -> billService.payGroupedTabs(List.of(1), new BigDecimal("49.99")));
        assertThrows(BusinessRuleException.class, () -> billService.payGroupedTabs(List.of(2), new BigDecimal("30.00")));
        assertThrows(BusinessRuleException.class, () -> billService.payGroupedTabs(List.of(9), new BigDecimal("30.00")));
        assertThrows(BusinessRuleException.class, () -> billService.payGroupedTabs(List.of(1), null));
        assertEquals(BillStatus.OPEN, b1.getStatus());
    }

    // ---------- reopenTab ----------

    @Test
    @DisplayName("Reabrir conta com mesa livre reocupa a mesa e mantém a associação")
    void reopenTab_freeTable_reoccupies() {
        Table t = table(5, 5, false);
        Bill b = bill(1, "10.00", "10.00");
        b.setStatus(BillStatus.CLOSED);
        b.getTables().add(t);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));

        billService.reopenTab(1);

        assertEquals(BillStatus.OPEN, b.getStatus());
        assertTrue(t.getOccupied());
        assertEquals(1, b.getTables().size());
    }

    @Test
    @DisplayName("Reabrir conta que não está fechada é bloqueado")
    void reopenTab_notClosed_throws() {
        Bill b = bill(1, "10.00", "0.00");
        when(billRepository.findById(1)).thenReturn(Optional.of(b));

        assertThrows(BusinessRuleException.class, () -> billService.reopenTab(1));
    }

    // ---------- consultas ----------

    @Test
    @DisplayName("getOpenTabs mapeia contas abertas com números de mesa e tipo de consumo")
    void getOpenTabs_mapsSummary() {
        Bill b = bill(1, "42.00", "0.00");
        b.setCustomerName("Mesa 7 - Cliente 1");
        b.getTables().add(table(7, 7, true));
        when(billRepository.findByStatus(BillStatus.OPEN)).thenReturn(List.of(b));

        List<BillSummaryDto> result = billService.getOpenTabs();

        assertEquals(1, result.size());
        assertEquals(List.of(7), result.get(0).tableNumbers());
        assertEquals(new BigDecimal("42.00"), result.get(0).totalAmount());
    }

    @Test
    @DisplayName("Parcial de consumo agrupa itens iguais e preserva a linha do tempo")
    void getTabPartial_groupsItems() {
        Bill b = bill(1, "40.00", "0.00");
        b.setCustomerName("João");
        Menu beer = new Menu();
        beer.setName("Cerveja");

        Order o1 = new Order(); o1.setId(1L); o1.setPrepStatus(PrepStatus.DELIVERED);
        Order o2 = new Order(); o2.setId(2L); o2.setPrepStatus(PrepStatus.QUEUED);
        OrderDetail d1 = new OrderDetail(); d1.setMenu(beer); d1.setQuantity(2); d1.setSoldPrice(new BigDecimal("10.00")); d1.setIsToGo(false);
        OrderDetail d2 = new OrderDetail(); d2.setMenu(beer); d2.setQuantity(2); d2.setSoldPrice(new BigDecimal("10.00")); d2.setIsToGo(false);

        when(billRepository.findById(1)).thenReturn(Optional.of(b));
        when(orderRepository.findByBillId(1)).thenReturn(List.of(o1, o2));
        when(orderDetailRepository.findByOrderId(1L)).thenReturn(List.of(d1));
        when(orderDetailRepository.findByOrderId(2L)).thenReturn(List.of(d2));

        BillPartialDto partial = billService.getTabPartial(1);

        assertEquals(2, partial.timeline().size());
        assertEquals(1, partial.groupedItems().size());
        assertEquals(4, partial.groupedItems().get(0).quantity());
        assertEquals(new BigDecimal("40.00"), partial.groupedItems().get(0).totalPrice());
    }

    @Test
    @DisplayName("Parcial de conta inexistente retorna 404 de negócio")
    void getTabPartial_notFound() {
        when(billRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> billService.getTabPartial(404));
    }

    @Test
    @DisplayName("Atualizar com nome nulo não altera o nome do cliente")
    void patchTab_nullName_keepsName() {
        Bill b = bill(1, "0.00", "0.00");
        b.setCustomerName("Original");
        when(billRepository.findById(1)).thenReturn(Optional.of(b));
        when(billRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        BillSummaryDto r = billService.patchTab(1, new com.syskewer.api.dto.salon.BillUpdateDto(null));

        assertEquals("Original", r.customerName());
    }
}
