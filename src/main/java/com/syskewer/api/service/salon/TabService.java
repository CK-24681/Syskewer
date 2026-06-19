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

    @Transactional
    public void archiveAsCredit(Integer tabId, String customerDocOrPhone) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        if (tab.getStatus() != TabStatus.OPEN) {
            throw new BusinessRuleException("Apenas comandas abertas podem ser arquivadas como fiado.");
        }
        if (customerDocOrPhone == null || customerDocOrPhone.trim().isEmpty()) {
            throw new BusinessRuleException("CPF ou Telefone é obrigatório para registrar pendência.");
        }
        tab.setCustomerName(tab.getCustomerName() + " (Devedor - Contato: " + customerDocOrPhone + ")");
        tab.setStatus(TabStatus.IN_DEBT);
        tab.setDeferredDate(LocalDateTime.now());
        tabRepository.save(tab);
        releaseTableIfEmpty(tab.getTable());
    }

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

    @Transactional
    public String registerPayment(Integer tabId, BigDecimal amountToPay, BigDecimal discount) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));

        if (tab.getStatus() == TabStatus.CLOSED) {
            throw new BusinessRuleException("Esta comanda já está totalmente paga e fechada.");
        }

        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
            if (!isAdmin) {
                throw new BusinessRuleException("Apenas o Administrador pode conceder descontos. Chame o gerente.");
            }
            tab.setTotalAmount(tab.getTotalAmount().subtract(discount));
        }

        BigDecimal balance = tab.getTotalAmount().subtract(tab.getPaidAmount());
        if (amountToPay.compareTo(balance) > 0) {
            throw new BusinessRuleException("O valor (R$ " + amountToPay + ") é maior que o saldo devedor atual (R$ " + balance + ").");
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

    private void releaseTableIfEmpty(Table table) {
        if (table != null) {
            boolean hasOpenTabs = tabRepository.existsByTableIdAndStatus(table.getId(), TabStatus.OPEN);
            if (!hasOpenTabs) {
                table.setOccupied(false);
                tableRepository.save(table);
            }
        }
    }

    @Transactional
    public void cancelTab(Integer tabId) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        if (tab.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Não é possível excluir uma comanda que já possui consumo. Cancele os itens primeiro ou realize o fechamento/pagamento.");
        }
        if (tab.getTable() != null) {
            Table table = tab.getTable();
            table.setOccupied(false);
            tableRepository.save(table);
        }
        tabRepository.delete(tab);
    }

    @Transactional
    public String transferTab(Integer tabId, Integer newTableNumber) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        Table newTable = tableRepository.findByNumber(newTableNumber)
                .orElseThrow(() -> new ResourceNotFoundException("A nova mesa informada não existe no salão."));
        if (newTable.getOccupied()) {
            throw new BusinessRuleException("Transferência negada: A mesa " + newTableNumber + " já está ocupada por outros clientes!");
        }
        Table oldTable = tab.getTable();
        if (oldTable != null) {
            oldTable.setOccupied(false);
            tableRepository.save(oldTable);
        }
        newTable.setOccupied(true);
        tableRepository.save(newTable);
        tab.setTable(newTable);
        tabRepository.save(tab);
        return "Comanda transferida com sucesso para a Mesa " + newTableNumber;
    }

    @Transactional
    public String payGroupedTabs(List<Integer> tabIds, BigDecimal amountReceived) {
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
        for (Tab tab : tabs) {
            tab.setPaidAmount(tab.getTotalAmount()); 
            tab.setStatus(TabStatus.CLOSED);
            releaseTableIfEmpty(tab.getTable());
            tabRepository.save(tab);
        }
        return "Sucesso! O pagamento agrupado de R$ " + totalBalance + " foi realizado e " + tabs.size() + " comandas foram liquidadas e fechadas.";
    }

    // MÉTODO READICIONADO PARA O TESTE E PARA O ADMIN
    @Transactional
    public String reopenTab(Integer tabId) {
        Tab tab = tabRepository.findById(tabId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));

        if (tab.getStatus() != TabStatus.CLOSED) {
            throw new BusinessRuleException("Esta comanda já está aberta ou com dívida.");
        }

        if (tab.getTable() != null) {
            if (tab.getTable().getOccupied()) {
                tab.setTable(null); 
            } else {
                tab.getTable().setOccupied(true);
                tableRepository.save(tab.getTable());
            }
        }

        tab.setStatus(TabStatus.OPEN);
        tabRepository.save(tab);

        return "Atenção: A comanda do cliente '" + tab.getCustomerName() + "' foi reaberta com sucesso.";
    }
}