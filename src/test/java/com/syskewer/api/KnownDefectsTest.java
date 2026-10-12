package com.syskewer.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.model.product.Menu;
import com.syskewer.api.model.salon.Bill;
import com.syskewer.api.model.salon.BillStatus;
import com.syskewer.api.model.salon.ConsumptionType;
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderDetail;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.model.salon.Table;
import com.syskewer.api.repository.product.MenuRepository;
import com.syskewer.api.repository.salon.BillRepository;
import com.syskewer.api.repository.salon.OrderDetailRepository;
import com.syskewer.api.repository.salon.OrderRepository;
import com.syskewer.api.repository.salon.TableRepository;
import com.syskewer.api.service.salon.BillService;
import com.syskewer.api.service.salon.OrderService;
import com.syskewer.api.service.salon.StoreSettingsService;

/**
 * Testes que descrevem o comportamento ESPERADO pelas regras de negócio e que
 * expõem defeitos reais encontrados na execução de testes (ver relatório de
 * avaliação de saída: docs/avaliacao_de_saida_relatorio.md).
 *
 * Cada teste está desabilitado ({@code @Disabled}) para não quebrar o build até
 * que o defeito seja corrigido. Para reproduzir: remover o {@code @Disabled}.
 * Evidência de falha registrada em docs/evidencias/.
 */
@Disabled("Defeitos conhecidos (DEF-01..07) - remover @Disabled para reproduzir")
@Tag("known-defect")
@ExtendWith(MockitoExtension.class)
class KnownDefectsTest {

    @Mock private BillRepository billRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderDetailRepository orderDetailRepository;
    @Mock private TableRepository tableRepository;
    @Mock private StoreSettingsService storeSettingsService;
    @Mock private MenuRepository menuRepository;

    @InjectMocks private BillService billService;

    private OrderService orderService() {
        return new OrderService(orderRepository, orderDetailRepository, billRepository, menuRepository,
                storeSettingsService);
    }

    private Bill bill(int id, String total) {
        Bill b = new Bill();
        b.setId(id);
        b.setCustomerName("Cliente " + id);
        b.setTotalAmount(new BigDecimal(total));
        b.setStatus(BillStatus.OPEN);
        b.setConsumptionType(ConsumptionType.MESA);
        return b;
    }

    @Test
    @DisplayName("DEF-01: remover couvert sucessivamente deve devolver exatamente o valor cobrado de cada conta")
    void removeCoverCharge_threeBills_eachRefundMatchesCharge() {
        Table t = new Table(10, true);
        t.setId(10);
        Bill b1 = bill(1, "1.67");
        Bill b2 = bill(2, "1.67");
        Bill b3 = bill(3, "1.67");
        for (Bill b : List.of(b1, b2, b3)) {
            b.setApplyCoverCharge(true);
            b.getTables().add(t);
        }
        when(billRepository.findById(1)).thenReturn(Optional.of(b1));
        when(billRepository.findById(2)).thenReturn(Optional.of(b2));
        when(billRepository.findByTableIdAndStatus(10, BillStatus.OPEN)).thenReturn(List.of(b1, b2, b3));

        billService.removeCoverCharge(1);
        billService.removeCoverCharge(2);

        // cada conta pagou R$ 1,67 de couvert -> ao remover, o total deve voltar a 0,00
        assertEquals(0, BigDecimal.ZERO.compareTo(b1.getTotalAmount()), "conta 1");
        assertEquals(0, BigDecimal.ZERO.compareTo(b2.getTotalAmount()), "conta 2 ficou com " + b2.getTotalAmount());
    }

    @Test
    @DisplayName("DEF-02: remover couvert de conta de balcão (sem mesa) deve abater o couvert aplicado")
    void removeCoverCharge_balcao_shouldRemoveCharge() {
        Bill b = bill(1, "15.00");
        b.setConsumptionType(ConsumptionType.BALCAO);
        b.setApplyCoverCharge(true);
        when(billRepository.findById(1)).thenReturn(Optional.of(b));

        billService.removeCoverCharge(1);

        assertEquals(new BigDecimal("10.00"), b.getTotalAmount());
        assertFalse(b.getApplyCoverCharge());
    }

    @Test
    @DisplayName("DEF-03: rachar item para conta em fiado (IN_DEBT) deve ser bloqueado")
    void splitItem_toInDebtBill_shouldBeBlocked() {
        Bill original = bill(1, "30.00");
        Bill inDebt = bill(2, "20.00");
        inDebt.setStatus(BillStatus.IN_DEBT);
        OrderDetail d = detail(original, "30.00");
        when(orderDetailRepository.findById(1L)).thenReturn(Optional.of(d));
        when(billRepository.findById(2)).thenReturn(Optional.of(inDebt));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        assertThrows(BusinessRuleException.class, () -> orderService().splitItem(1L, List.of(2)));
    }

    @Test
    @DisplayName("DEF-04: rachar item informando a própria conta como destino deve ser bloqueado")
    void splitItem_toSameBill_shouldBeBlocked() {
        Bill original = bill(1, "30.00");
        OrderDetail d = detail(original, "30.00");
        when(orderDetailRepository.findById(1L)).thenReturn(Optional.of(d));
        when(billRepository.findById(1)).thenReturn(Optional.of(original));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        assertThrows(BusinessRuleException.class, () -> orderService().splitItem(1L, List.of(1)));
    }

    @Test
    @DisplayName("DEF-05: pagamento agrupado com ID inexistente deve ser rejeitado (hoje ignora o ID silenciosamente)")
    void payGroupedTabs_unknownId_shouldBeRejected() {
        Bill b1 = bill(1, "50.00");
        b1.setPaidAmount(BigDecimal.ZERO);
        when(billRepository.findAllById(List.of(1, 999))).thenReturn(List.of(b1));

        assertThrows(BusinessRuleException.class,
                () -> billService.payGroupedTabs(List.of(1, 999), new BigDecimal("50.00")));
    }

    @Test
    @DisplayName("DEF-06: soma dos couverts divididos deve ser exatamente R$ 5,00 (hoje: 3 x 1,67 = 5,01)")
    void toggleCoverCharge_threeBills_sumMustEqualCouvert() {
        Table t = new Table(10, true);
        t.setId(10);
        Bill b1 = bill(1, "0.00");
        Bill b2 = bill(2, "0.00");
        Bill b3 = bill(3, "0.00");
        for (Bill b : List.of(b1, b2, b3)) {
            b.getTables().add(t);
        }
        when(billRepository.findById(1)).thenReturn(Optional.of(b1));
        when(billRepository.findByTableIdAndStatus(10, BillStatus.OPEN)).thenReturn(List.of(b1, b2, b3));

        billService.toggleCoverCharge(1);

        BigDecimal sum = b1.getTotalAmount().add(b2.getTotalAmount()).add(b3.getTotalAmount());
        assertEquals(0, new BigDecimal("5.00").compareTo(sum), "soma cobrada: " + sum);
    }

    @Test
    @DisplayName("DEF-07: serviço deve rejeitar pagamento negativo (defesa em profundidade; DTO já bloqueia na API)")
    void registerPayment_negativeAmount_shouldBeRejected() {
        Bill b = bill(1, "100.00");
        b.setPaidAmount(new BigDecimal("50.00"));
        when(billRepository.findById(1)).thenReturn(Optional.of(b));

        assertThrows(BusinessRuleException.class,
                () -> billService.registerPayment(1, new BigDecimal("-20.00"), null));
    }

    private OrderDetail detail(Bill original, String price) {
        Order o = new Order();
        o.setBill(original);
        o.setPrepStatus(PrepStatus.DELIVERED);
        OrderDetail d = new OrderDetail();
        d.setOrder(o);
        d.setQuantity(1);
        d.setSoldPrice(new BigDecimal(price));
        Menu m = new Menu();
        m.setName("Porção");
        d.setMenu(m);
        return d;
    }
}
