package com.syskewer.api.service.salon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syskewer.api.dto.salon.PartialItemDto;
import com.syskewer.api.dto.salon.TabDeliveryDto;
import com.syskewer.api.dto.salon.TabOpenDto;
import com.syskewer.api.dto.salon.TabPartialDto;
import com.syskewer.api.dto.salon.TabSummaryDto;
import com.syskewer.api.dto.salon.TabUpdateDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.salon.ConsumptionType;
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderItem;
import com.syskewer.api.model.salon.Tab;
import com.syskewer.api.model.salon.TabStatus;
import com.syskewer.api.model.salon.Table;
import com.syskewer.api.repository.salon.OrderItemRepository;
import com.syskewer.api.repository.salon.OrderRepository;
import com.syskewer.api.repository.salon.TabRepository;
import com.syskewer.api.repository.salon.TableRepository;

// Servico para gerenciar comandas de mesa e balcao
@Service
public class TabService {

    private static final BigDecimal COUVERT_VALUE = new BigDecimal("5.00");
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("5.00");

    private final TabRepository tabRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TableRepository tableRepository;
    private final StoreSettingsService storeSettingsService;

    public TabService(TabRepository tabRepository, OrderRepository orderRepository,
            OrderItemRepository orderItemRepository, TableRepository tableRepository,
            StoreSettingsService storeSettingsService) {
        this.tabRepository = tabRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.tableRepository = tableRepository;
        this.storeSettingsService = storeSettingsService;
    }

    // Retorna a lista de todas as comandas que estao abertas
    public List<TabSummaryDto> getOpenTabs() {
        return tabRepository.findByStatus(TabStatus.OPEN).stream().map(tab -> {
            Integer tableNum = (tab.getTable() != null) ? tab.getTable().getNumber() : null;
            return new TabSummaryDto(
                    tab.getId(),
                    tab.getCustomerName(),
                    tableNum,
                    tab.getConsumptionType().name(),
                    tab.getTotalAmount());
        }).collect(Collectors.toList());
    }

    // Calcula e retorna a parcial de consumo e itens da comanda
    public TabPartialDto getTabPartial(Integer tabId) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda ID " + tabId + " não encontrada"));

        List<Order> orders = orderRepository.findByTabId(tabId);
        List<PartialItemDto> timeline = new ArrayList<>();
        Map<String, PartialItemDto> groupedItems = new LinkedHashMap<>();

        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                BigDecimal itemTotalPrice = item.getSoldPrice().multiply(new BigDecimal(item.getQuantity()));
                
                PartialItemDto itemDto = new PartialItemDto(
                        item.getProduct().getName(), item.getQuantity(), item.getSoldPrice(), itemTotalPrice,
                        item.getIsToGo(), order.getPrepStatus().name());

                timeline.add(itemDto);

                String key = item.getProduct().getName() + (Boolean.TRUE.equals(item.getIsToGo()) ? " (Viagem)" : "");
                if (groupedItems.containsKey(key)) {
                    PartialItemDto existing = groupedItems.get(key);
                    int newQty = existing.quantity() + item.getQuantity();
                    BigDecimal newTotal = existing.totalPrice().add(itemTotalPrice);
                    groupedItems.put(key, new PartialItemDto(existing.productName(), newQty, existing.unitPrice(),
                            newTotal, existing.isToGo(), "-"));
                } else {
                    groupedItems.put(key, itemDto);
                }
            }
        }
        return new TabPartialDto(tab.getId(), tab.getCustomerName(), tab.getTotalAmount(), timeline,
                new ArrayList<>(groupedItems.values()));
    }

    // Fecha a comanda se o saldo estiver quitado
    @Transactional
    public void closeTab(Integer tabId) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        if (tab.getStatus() == TabStatus.CLOSED) {
            throw new BusinessRuleException("Esta comanda já foi fechada!");
        }
        BigDecimal balance = tab.getTotalAmount().subtract(tab.getPaidAmount());
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("A comanda não pode ser fechada pois possui um saldo devedor de R$ "
                    + balance + ". Utilize o registro de pagamento.");
        }
        tab.setStatus(TabStatus.CLOSED);
        tabRepository.save(tab);
        releaseTableIfEmpty(tab.getTable());
    }

    // Aplica o valor de couvert artistico na comanda
    @Transactional
    public void toggleCoverCharge(Integer tabId) {
        Tab currentTab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        if (currentTab.getStatus() != TabStatus.OPEN) {
            throw new BusinessRuleException("Só é possível alterar o couvert de comandas abertas.");
        }

        if (currentTab.getTable() != null) {
            List<Tab> tableTabs = tabRepository.findByTableIdAndStatus(currentTab.getTable().getId(), TabStatus.OPEN);
            boolean alreadyApplied = tableTabs.stream().anyMatch(Tab::getApplyCoverCharge);
            if (alreadyApplied) {
                throw new BusinessRuleException("O couvert já foi aplicado nesta mesa.");
            }
            BigDecimal splitValue = COUVERT_VALUE.divide(new BigDecimal(tableTabs.size()), 2, java.math.RoundingMode.HALF_UP);
            for (Tab t : tableTabs) {
                t.setApplyCoverCharge(true);
                t.setTotalAmount(t.getTotalAmount().add(splitValue));
                tabRepository.save(t);
            }
        } else {
            if (currentTab.getApplyCoverCharge()) {
                throw new BusinessRuleException("O couvert já foi aplicado nesta comanda.");
            }
            currentTab.setApplyCoverCharge(true);
            currentTab.setTotalAmount(currentTab.getTotalAmount().add(COUVERT_VALUE));
            tabRepository.save(currentTab);
        }
    }

    // Remove o valor de couvert artistico da comanda
    @Transactional
    public void removeCoverCharge(Integer tabId) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        
        if (tab.getStatus() == TabStatus.CLOSED) {
            throw new BusinessRuleException("Comanda fechada.");
        }

        if (Boolean.TRUE.equals(tab.getApplyCoverCharge()) && tab.getTable() != null) {
            List<Tab> tableTabs = tabRepository.findByTableIdAndStatus(tab.getTable().getId(), TabStatus.OPEN);
            long count = tableTabs.stream().filter(Tab::getApplyCoverCharge).count();
            if (count > 0) {
                BigDecimal splitValue = COUVERT_VALUE.divide(new BigDecimal(count), 2, java.math.RoundingMode.HALF_UP);
                tab.setTotalAmount(tab.getTotalAmount().subtract(splitValue));
                tab.setApplyCoverCharge(false);
                tabRepository.save(tab);
            }
        }
    }

    // Arquiva a comanda como fiado vinculando o CPF ou telefone do devedor
    @Transactional
    public void archiveAsCredit(Integer tabId, String customerDocOrPhone) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        if (tab.getStatus() != TabStatus.OPEN) {
            throw new BusinessRuleException("Apenas comandas abertas podem ser arquivadas como fiado.");
        }
        BigDecimal balance = tab.getTotalAmount().subtract(tab.getPaidAmount());
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Não é possível arquivar como fiado uma comanda sem saldo devedor.");
        }
        if (customerDocOrPhone == null || customerDocOrPhone.trim().isEmpty()) {
            throw new BusinessRuleException("CPF ou Telefone é obrigatório para registrar pendência.");
        }
        tab.setCustomerDocOrPhone(customerDocOrPhone.trim());
        tab.setStatus(TabStatus.IN_DEBT);
        tab.setDeferredDate(LocalDateTime.now());
        tabRepository.save(tab);
        releaseTableIfEmpty(tab.getTable());
    }

    // Abre uma nova comanda de mesa ou balcao validando se o estabelecimento esta aberto
    @Transactional
    public TabSummaryDto openTab(TabOpenDto dto) {
        if (!storeSettingsService.isStoreOpen()) {
            throw new BusinessRuleException("O bar está fechado! Não é possível abrir novas mesas/comandas.");
        }

        Tab tab = new Tab();
        tab.setConsumptionType(dto.consumptionType());
        
        // BUSCA A MESA PRIMEIRO PARA TER O ID CORRETO
        if (dto.consumptionType() == ConsumptionType.MESA) {
            Table table = tableRepository.findByNumber(dto.tableNumber())
                    .orElseThrow(() -> new BusinessRuleException("Mesa " + dto.tableNumber() + " não encontrada no salão!"));
            tab.setTable(table);
            
            if (!table.getOccupied()) {
                table.setOccupied(true);
                tableRepository.save(table);
            }
        }

        // AGORA GERA O NOME COM O ID CORRETO DA MESA
        if (dto.customerName() == null || dto.customerName().trim().isEmpty()) {
            if (dto.consumptionType() == ConsumptionType.MESA) {
                List<Tab> abertas = tabRepository.findByTableIdAndStatus(tab.getTable().getId(), TabStatus.OPEN);
                tab.setCustomerName("Mesa " + dto.tableNumber() + " - Cliente " + (abertas.size() + 1));
            } else {
                tab.setCustomerName("Cliente Balcão");
            }
        } else {
            tab.setCustomerName(dto.customerName().trim());
        }

        tab = tabRepository.save(tab);
        Integer tableNum = (tab.getTable() != null) ? tab.getTable().getNumber() : null;
        return new TabSummaryDto(tab.getId(), tab.getCustomerName(), tableNum, tab.getConsumptionType().name(), tab.getTotalAmount());
    }

    // Registra um pagamento parcial ou total para diminuir o saldo devedor
    @Transactional
    public String registerPayment(Integer tabId, BigDecimal amountToPay, BigDecimal discount) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));

        if (tab.getStatus() == TabStatus.CLOSED) {
            throw new BusinessRuleException("Esta comanda já está totalmente paga e fechada.");
        }

        BigDecimal originalBalance = tab.getTotalAmount().subtract(tab.getPaidAmount());

        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
            if (!isAdmin) {
                throw new BusinessRuleException("Apenas o Administrador pode conceder descontos. Chame o gerente.");
            }
            if (discount.compareTo(originalBalance) > 0) {
                throw new BusinessRuleException("O valor do desconto (R$ " + discount + ") não pode ser maior que o saldo devedor atual (R$ " + originalBalance + ").");
            }
            tab.setTotalAmount(tab.getTotalAmount().subtract(discount));
        }

        BigDecimal balance = tab.getTotalAmount().subtract(tab.getPaidAmount());
        if (amountToPay.compareTo(balance) > 0) {
            throw new BusinessRuleException("O valor do pagamento (R$ " + amountToPay + ") é maior que o saldo devedor atual (R$ " + balance + ").");
        }

        tab.setPaidAmount(tab.getPaidAmount().add(amountToPay));
        BigDecimal newBalance = tab.getTotalAmount().subtract(tab.getPaidAmount());

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            tab.setStatus(TabStatus.CLOSED);
            releaseTableIfEmpty(tab.getTable());
            tabRepository.save(tab);
            return "Pagamento final recebido com sucesso. A comanda foi fechada!";
        }

        tabRepository.save(tab);
        return "Pagamento parcial registrado. Saldo devedor restante: R$ " + newBalance + ".";
    }

    // Abre uma comanda de delivery com endereco e taxa de entrega fixa
    @Transactional
    public TabSummaryDto openDeliveryTab(TabDeliveryDto dto) {
        Tab tab = new Tab();
        tab.setCustomerName(dto.customerName().trim());
        tab.setConsumptionType(ConsumptionType.DELIVERY);
        tab.setDeliveryAddress(dto.deliveryAddress().trim());
        tab.setDeliveryFee(DELIVERY_FEE);
        tab.setTotalAmount(DELIVERY_FEE);
        tab = tabRepository.save(tab);
        return new TabSummaryDto(tab.getId(), tab.getCustomerName(), null, tab.getConsumptionType().name(), tab.getTotalAmount());
    }

    // Libera a mesa do salao se nao existirem outras comandas abertas nela
    private void releaseTableIfEmpty(Table table) {
        if (table != null) {
            boolean hasOpenTabs = tabRepository.existsByTableIdAndStatus(table.getId(), TabStatus.OPEN);
            if (!hasOpenTabs) {
                table.setOccupied(false);
                tableRepository.save(table);
            }
        }
    }

    // Cancela e exclui uma comanda sem consumo
    @Transactional
    public void cancelTab(Integer tabId) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        if (tab.getStatus() == TabStatus.CLOSED) {
            throw new BusinessRuleException("Não é possível excluir uma comanda já fechada.");
        }
        if (tab.getStatus() == TabStatus.IN_DEBT) {
            throw new BusinessRuleException("Não é possível excluir uma comanda arquivada como fiado.");
        }
        if (tab.getTotalAmount().compareTo(BigDecimal.ZERO) > 0 || tab.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Não é possível excluir uma comanda que já possui consumo ou pagamentos registrados. Cancele os itens primeiro.");
        }
        if (tab.getTable() != null) {
            Table table = tab.getTable();
            table.setOccupied(false);
            tableRepository.save(table);
        }
        tabRepository.delete(tab);
    }

    // Transfere a comanda para outra mesa vazia
    @Transactional
    public String transferTab(Integer tabId, Integer newTableNumber) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        if (tab.getStatus() != TabStatus.OPEN) {
            throw new BusinessRuleException("Apenas comandas abertas podem ser transferidas de mesa.");
        }
        Table newTable = tableRepository.findByNumber(newTableNumber)
                .orElseThrow(() -> new ResourceNotFoundException("A nova mesa informada não existe no salão."));
        if (newTable.getOccupied()) {
            throw new BusinessRuleException("Transferência negada: A mesa " + newTableNumber + " já está ocupada por outros clientes!");
        }
        Table oldTable = tab.getTable();
        newTable.setOccupied(true);
        tableRepository.save(newTable);
        tab.setTable(newTable);
        tab.setConsumptionType(ConsumptionType.MESA);
        tabRepository.save(tab);
        if (oldTable != null) {
            releaseTableIfEmpty(oldTable);
        }
        return "Comanda transferida com sucesso para a Mesa " + newTableNumber;
    }

    // Liquida o pagamento de varias comandas ao mesmo tempo e calcula o troco
    @Transactional
    public String payGroupedTabs(List<Integer> tabIds, BigDecimal amountReceived) {
        if (amountReceived == null || amountReceived.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("O valor recebido deve ser maior que zero.");
        }
        List<Tab> tabs = tabRepository.findAllById(tabIds);
        if (tabs.isEmpty()) {
            throw new BusinessRuleException("Nenhuma comanda selecionada.");
        }
        BigDecimal totalBalance = BigDecimal.ZERO;
        for (Tab tab : tabs) {
            if (tab.getStatus() == TabStatus.CLOSED) {
                throw new BusinessRuleException("A comanda ID " + tab.getId() + " já está fechada e não pode ser agrupada.");
            }
            BigDecimal balance = tab.getTotalAmount().subtract(tab.getPaidAmount());
            totalBalance = totalBalance.add(balance);
        }
        if (amountReceived.compareTo(totalBalance) < 0) {
            throw new BusinessRuleException("O valor recebido (R$ " + amountReceived + ") é menor que o saldo das comandas (R$ " + totalBalance + ").");
        }
        BigDecimal change = amountReceived.subtract(totalBalance);
        String changeMsg = change.compareTo(BigDecimal.ZERO) > 0 ? " Troco: R$ " + change + "." : "";
        for (Tab tab : tabs) {
            tab.setPaidAmount(tab.getTotalAmount()); 
            tab.setStatus(TabStatus.CLOSED);
            releaseTableIfEmpty(tab.getTable());
            tabRepository.save(tab);
        }
        return "Sucesso! O pagamento agrupado de R$ " + totalBalance + " foi realizado e " + tabs.size() + " comandas foram liquidadas e fechadas." + changeMsg;
    }

    // MÉTODO READICIONADO PARA O TESTE E PARA O ADMIN
    // Reabre uma comanda fechada (caso a mesa esteja ocupada ela vira consumo de balcao)
    @Transactional
    public String reopenTab(Integer tabId) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));

        if (tab.getStatus() != TabStatus.CLOSED) {
            throw new BusinessRuleException("Esta comanda já está aberta ou com dívida.");
        }

        boolean lostTable = false;
        if (tab.getTable() != null) {
            if (tab.getTable().getOccupied()) {
                lostTable = true;
                tab.setTable(null); 
            } else {
                tab.getTable().setOccupied(true);
                tableRepository.save(tab.getTable());
            }
        }

        tab.setStatus(TabStatus.OPEN);
        tabRepository.save(tab);

        if (lostTable) {
            return "Atenção: A comanda do cliente '" + tab.getCustomerName() + "' foi reaberta com sucesso. Nota: A mesa original estava ocupada, então a comanda foi reaberta sem mesa vinculada (consumo de balcão).";
        }
        return "Atenção: A comanda do cliente '" + tab.getCustomerName() + "' foi reaberta com sucesso.";
    }

    // Atualiza os dados cadastrais da comanda como o nome do cliente
    @Transactional
    public TabSummaryDto patchTab(Integer tabId, TabUpdateDto dto) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));

        if (dto.customerName() != null) {
            tab.setCustomerName(dto.customerName().trim());
        }

        tab = tabRepository.save(tab);
        Integer tableNum = (tab.getTable() != null) ? tab.getTable().getNumber() : null;
        return new TabSummaryDto(tab.getId(), tab.getCustomerName(), tableNum, tab.getConsumptionType().name(), tab.getTotalAmount());
    }
}