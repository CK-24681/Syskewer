package com.syskewer.api.service.salon;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syskewer.api.dto.salon.PartialItemDto;
import com.syskewer.api.dto.salon.TabPartialDto;
import com.syskewer.api.dto.salon.TabSummaryDto;
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderItem;
import com.syskewer.api.model.salon.Tab;
import com.syskewer.api.model.salon.TabStatus;
import com.syskewer.api.model.salon.Table;
import com.syskewer.api.repository.salon.OrderItemRepository;
import com.syskewer.api.repository.salon.OrderRepository;
import com.syskewer.api.repository.salon.TabRepository;
import com.syskewer.api.repository.salon.TableRepository;

/** Regras de comanda: abertura, pagamento, couvert, fiado e liberação de mesa. */
@Service
public class TabService {

    private static final BigDecimal COUVERT_VALUE = new BigDecimal("5.00");
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("5.00");

    private final TabRepository tabRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TableRepository tableRepository;

    public TabService(TabRepository tabRepository, OrderRepository orderRepository,
                      OrderItemRepository orderItemRepository, TableRepository tableRepository) {
        this.tabRepository = tabRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.tableRepository = tableRepository;
    }

    /** @return comandas com status OPEN */
    public List<TabSummaryDto> getOpenTabs() {
        return tabRepository.findByStatus(TabStatus.OPEN).stream().map(tab -> {
            Integer tableNum = (tab.getTable() != null) ? tab.getTable().getNumber() : null;
            return new TabSummaryDto(
                    tab.getId(),
                    tab.getCustomerName(),
                    tableNum,
                    tab.getConsumptionType().name(),
                    tab.getTotalAmount()
            );
        }).collect(Collectors.toList());
    }

    /**
     * @param tabId id da comanda
     * @return itens consumidos e total parcial
     */
    public TabPartialDto getTabPartial(Integer tabId) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new RuntimeException("Comanda ID " + tabId + " não encontrada"));

        List<Order> orders = orderRepository.findByTabId(tabId);
        List<PartialItemDto> itemsList = new ArrayList<>();

        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                BigDecimal itemTotalPrice = item.getSoldPrice().multiply(new BigDecimal(item.getQuantity()));
                itemsList.add(new PartialItemDto(
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getSoldPrice(),
                        itemTotalPrice,
                        item.getIsToGo()
                ));
            }
        }

        return new TabPartialDto(
                tab.getId(),
                tab.getCustomerName(),
                tab.getTotalAmount(),
                itemsList
        );
    }

    /**
     * Fecha a comanda e libera a mesa.
     *
     * @param tabId id da comanda
     */
    @Transactional
    public void closeTab(Integer tabId) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new RuntimeException("Comanda não encontrada!"));

        if (tab.getStatus() == TabStatus.CLOSED) {
            throw new RuntimeException("Esta comanda já foi fechada e paga!");
        }

        tab.setStatus(TabStatus.CLOSED);
        tab.setPaidAmount(tab.getTotalAmount());
        tabRepository.save(tab);

        releaseTable(tab);
    }

    /**
     * Alterna couvert artístico (R$ 5,00 fixo) no total.
     *
     * @param tabId id da comanda
     */
    @Transactional
    public void toggleCoverCharge(Integer tabId) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new RuntimeException("Comanda não encontrada!"));

        if (tab.getStatus() != TabStatus.OPEN) {
            throw new RuntimeException("Só é possível alterar o couvert de comandas abertas.");
        }

        if (tab.getApplyCoverCharge()) {
            tab.setApplyCoverCharge(false);
            tab.setTotalAmount(tab.getTotalAmount().subtract(COUVERT_VALUE));
        } else {
            tab.setApplyCoverCharge(true);
            tab.setTotalAmount(tab.getTotalAmount().add(COUVERT_VALUE));
        }
        tabRepository.save(tab);
    }

    /**
     * Arquiva como fiado e libera a mesa — exige CPF ou telefone para cobrança futura.
     *
     * @param tabId id da comanda
     * @param customerDocOrPhone documento ou telefone do cliente
     */
    @Transactional
    public void archiveAsCredit(Integer tabId, String customerDocOrPhone) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new RuntimeException("Comanda não encontrada!"));

        if (tab.getStatus() != TabStatus.OPEN) {
            throw new RuntimeException("Apenas comandas abertas podem ser arquivadas como fiado.");
        }

        if (customerDocOrPhone == null || customerDocOrPhone.trim().isEmpty()) {
            throw new RuntimeException("CPF ou Telefone é obrigatório para registrar pendência.");
        }

        tab.setCustomerName(tab.getCustomerName() + " (Devedor - Contato: " + customerDocOrPhone + ")");
        tab.setStatus(TabStatus.IN_DEBT);
        tab.setDeferredDate(java.time.LocalDateTime.now());

        tabRepository.save(tab);
        releaseTable(tab);
    }

    /**
     * @param dto dados de abertura no salão
     * @return resumo da comanda criada
     */
    @Transactional
    public TabSummaryDto openTab(com.syskewer.api.dto.salon.TabOpenDto dto) {
        Tab tab = new Tab();

        tab.setCustomerName(dto.customerName() != null && !dto.customerName().trim().isEmpty()
                ? dto.customerName().trim() : null);
        tab.setConsumptionType(dto.consumptionType());

        if (dto.tableNumber() != null) {
            Table table = tableRepository.findByNumber(dto.tableNumber())
                    .orElseThrow(() -> new RuntimeException("Mesa " + dto.tableNumber() + " não encontrada no salão!"));

            tab.setTable(table);

            if (!table.getOccupied()) {
                table.setOccupied(true);
                tableRepository.save(table);
            }
        } else if (dto.consumptionType() == com.syskewer.api.model.salon.ConsumptionType.MESA) {
            throw new RuntimeException("É obrigatório informar o número da mesa para o consumo do tipo MESA.");
        }

        tab = tabRepository.save(tab);

        Integer tableNum = (tab.getTable() != null) ? tab.getTable().getNumber() : null;
        return new TabSummaryDto(
                tab.getId(),
                tab.getCustomerName(),
                tableNum,
                tab.getConsumptionType().name(),
                tab.getTotalAmount()
        );
    }

    /**
     * Registra pagamento parcial ou quita a conta.
     *
     * @param tabId id da comanda
     * @param amountToPay valor recebido
     * @return mensagem com saldo restante ou confirmação de fechamento
     */
    @Transactional
    public String registerPayment(Integer tabId, BigDecimal amountToPay) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new RuntimeException("Comanda não encontrada!"));

        if (tab.getStatus() != TabStatus.OPEN) {
            throw new RuntimeException("Apenas comandas abertas podem receber pagamentos.");
        }

        BigDecimal balance = tab.getTotalAmount().subtract(tab.getPaidAmount());

        if (amountToPay.compareTo(balance) > 0) {
             throw new RuntimeException("O valor (R$ " + amountToPay + ") é maior que o saldo devedor atual (R$ " + balance + ").");
        }

        tab.setPaidAmount(tab.getPaidAmount().add(amountToPay));
        BigDecimal newBalance = tab.getTotalAmount().subtract(tab.getPaidAmount());

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            tab.setStatus(TabStatus.CLOSED);
            releaseTable(tab);
            tabRepository.save(tab);
            return "Pagamento final de R$ " + amountToPay + " recebido com sucesso. A comanda foi fechada!";
        }

        tabRepository.save(tab);
        return "Pagamento parcial de R$ " + amountToPay + " recebido. Saldo devedor restante: R$ " + newBalance + ".";
    }

    /**
     * Abre comanda de delivery — taxa de R$ 5,00 já entra no total.
     *
     * @param dto nome, endereço e dados do cliente
     * @return resumo da comanda criada
     */
    @Transactional
    public TabSummaryDto openDeliveryTab(com.syskewer.api.dto.salon.TabDeliveryDto dto) {
        Tab tab = new Tab();

        tab.setCustomerName(dto.customerName().trim());
        tab.setConsumptionType(com.syskewer.api.model.salon.ConsumptionType.DELIVERY);
        tab.setDeliveryAddress(dto.deliveryAddress().trim());
        tab.setDeliveryFee(DELIVERY_FEE);
        tab.setTotalAmount(DELIVERY_FEE);

        tab = tabRepository.save(tab);

        return new TabSummaryDto(
                tab.getId(),
                tab.getCustomerName(),
                null,
                tab.getConsumptionType().name(),
                tab.getTotalAmount()
        );
    }

    private void releaseTable(Tab tab) {
        if (tab.getTable() != null) {
            Table table = tab.getTable();
            table.setOccupied(false);
            tableRepository.save(table);
        }
    }
}
