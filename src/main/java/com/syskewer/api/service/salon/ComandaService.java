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
import com.syskewer.api.dto.salon.ComandaDeliveryDto;
import com.syskewer.api.dto.salon.ComandaOpenDto;
import com.syskewer.api.dto.salon.ComandaPartialDto;
import com.syskewer.api.dto.salon.ComandaSummaryDto;
import com.syskewer.api.dto.salon.ComandaUpdateDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.salon.ConsumptionType;
import com.syskewer.api.model.salon.ComandaItem;
import com.syskewer.api.model.salon.ComandaItemDetail;
import com.syskewer.api.model.salon.Comanda;
import com.syskewer.api.model.salon.ComandaStatus;
import com.syskewer.api.model.salon.Table;
import com.syskewer.api.repository.salon.ComandaItemDetailRepository;
import com.syskewer.api.repository.salon.ComandaItemRepository;
import com.syskewer.api.repository.salon.ComandaRepository;
import com.syskewer.api.repository.salon.TableRepository;

@Service
public class ComandaService {

    private static final BigDecimal COUVERT_VALUE = new BigDecimal("5.00");
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("5.00");

    private final ComandaRepository comandaRepository;
    private final ComandaItemRepository comandaItemRepository;
    private final ComandaItemDetailRepository comandaItemDetailRepository;
    private final TableRepository tableRepository;
    private final StoreSettingsService storeSettingsService;

    public ComandaService(ComandaRepository comandaRepository, ComandaItemRepository comandaItemRepository,
            ComandaItemDetailRepository comandaItemDetailRepository, TableRepository tableRepository,
            StoreSettingsService storeSettingsService) {
        this.comandaRepository = comandaRepository;
        this.comandaItemRepository = comandaItemRepository;
        this.comandaItemDetailRepository = comandaItemDetailRepository;
        this.tableRepository = tableRepository;
        this.storeSettingsService = storeSettingsService;
    }

    // Retorna a lista de todas as comandas que estao abertas
    public List<ComandaSummaryDto> getOpenTabs() {
        return comandaRepository.findByStatus(ComandaStatus.OPEN).stream().map(comanda -> {
            List<Integer> tableNums = comanda.getTables().stream().map(Table::getNumber).collect(Collectors.toList());
            return new ComandaSummaryDto(
                    comanda.getId(),
                    comanda.getCustomerName(),
                    tableNums,
                    comanda.getConsumptionType().name(),
                    comanda.getTotalAmount());
        }).collect(Collectors.toList());
    }

    // Calcula e retorna a parcial de consumo e itens da comanda
    public ComandaPartialDto getTabPartial(Integer comandaId) {
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda ID " + comandaId + " não encontrada"));

        List<ComandaItem> itemsComanda = comandaItemRepository.findByComandaId(comandaId);
        List<PartialItemDto> timeline = new ArrayList<>();
        Map<String, PartialItemDto> groupedItems = new LinkedHashMap<>();

        for (ComandaItem comandaItem : itemsComanda) {
            List<ComandaItemDetail> details = comandaItemDetailRepository.findByComandaItemId(comandaItem.getId());
            for (ComandaItemDetail detail : details) {
                BigDecimal itemTotalPrice = detail.getSoldPrice().multiply(new BigDecimal(detail.getQuantity()));
                
                PartialItemDto itemDto = new PartialItemDto(
                        detail.getMenu().getName(), detail.getQuantity(), detail.getSoldPrice(), itemTotalPrice,
                        detail.getIsToGo(), comandaItem.getPrepStatus().name());

                timeline.add(itemDto);

                String key = detail.getMenu().getName() + (Boolean.TRUE.equals(detail.getIsToGo()) ? " (Viagem)" : "");
                if (groupedItems.containsKey(key)) {
                    PartialItemDto existing = groupedItems.get(key);
                    int newQty = existing.quantity() + detail.getQuantity();
                    BigDecimal newTotal = existing.totalPrice().add(itemTotalPrice);
                    groupedItems.put(key, new PartialItemDto(existing.productName(), newQty, existing.unitPrice(),
                            newTotal, existing.isToGo(), "-"));
                } else {
                    groupedItems.put(key, itemDto);
                }
            }
        }
        return new ComandaPartialDto(comanda.getId(), comanda.getCustomerName(), comanda.getTotalAmount(), timeline,
                new ArrayList<>(groupedItems.values()));
    }

    // Fecha a comanda se o saldo estiver quitado
    @Transactional
    public void closeTab(Integer comandaId) {
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        if (comanda.getStatus() == ComandaStatus.CLOSED) {
            throw new BusinessRuleException("Esta comanda já foi fechada!");
        }
        BigDecimal balance = comanda.getTotalAmount().subtract(comanda.getPaidAmount());
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("A comanda não pode ser fechada pois possui um saldo devedor de R$ "
                    + balance + ". Utilize o registro de pagamento.");
        }
        comanda.setStatus(ComandaStatus.CLOSED);
        comandaRepository.save(comanda);
        for (Table table : comanda.getTables()) {
            releaseTableIfEmpty(table);
        }
    }

    // Aplica o valor de couvert artistico na comanda
    @Transactional
    public void toggleCoverCharge(Integer comandaId) {
        Comanda currentComanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        if (currentComanda.getStatus() != ComandaStatus.OPEN) {
            throw new BusinessRuleException("Só é possível alterar o couvert de comandas abertas.");
        }

        if (!currentComanda.getTables().isEmpty()) {
            // Se tiver mesas associadas, distribui o couvert entre todas as comandas abertas nessas mesas
            List<Comanda> tableComandas = new ArrayList<>();
            for (Table table : currentComanda.getTables()) {
                tableComandas.addAll(comandaRepository.findByTableIdAndStatus(table.getId(), ComandaStatus.OPEN));
            }
            // Evita duplicatas
            tableComandas = tableComandas.stream().distinct().collect(Collectors.toList());

            boolean alreadyApplied = tableComandas.stream().anyMatch(Comanda::getApplyCoverCharge);
            if (alreadyApplied) {
                throw new BusinessRuleException("O couvert já foi aplicado nesta mesa.");
            }
            BigDecimal splitValue = COUVERT_VALUE.divide(new BigDecimal(tableComandas.size()), 2, java.math.RoundingMode.HALF_UP);
            for (Comanda c : tableComandas) {
                c.setApplyCoverCharge(true);
                c.setTotalAmount(c.getTotalAmount().add(splitValue));
                comandaRepository.save(c);
            }
        } else {
            if (currentComanda.getApplyCoverCharge()) {
                throw new BusinessRuleException("O couvert já foi aplicado nesta comanda.");
            }
            currentComanda.setApplyCoverCharge(true);
            currentComanda.setTotalAmount(currentComanda.getTotalAmount().add(COUVERT_VALUE));
            comandaRepository.save(currentComanda);
        }
    }

    // Remove o valor de couvert artistico da comanda
    @Transactional
    public void removeCoverCharge(Integer comandaId) {
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        
        if (comanda.getStatus() == ComandaStatus.CLOSED) {
            throw new BusinessRuleException("Comanda fechada.");
        }

        if (Boolean.TRUE.equals(comanda.getApplyCoverCharge()) && !comanda.getTables().isEmpty()) {
            List<Comanda> tableComandas = new ArrayList<>();
            for (Table table : comanda.getTables()) {
                tableComandas.addAll(comandaRepository.findByTableIdAndStatus(table.getId(), ComandaStatus.OPEN));
            }
            tableComandas = tableComandas.stream().distinct().collect(Collectors.toList());

            long count = tableComandas.stream().filter(Comanda::getApplyCoverCharge).count();
            if (count > 0) {
                BigDecimal splitValue = COUVERT_VALUE.divide(new BigDecimal(count), 2, java.math.RoundingMode.HALF_UP);
                comanda.setTotalAmount(comanda.getTotalAmount().subtract(splitValue));
                comanda.setApplyCoverCharge(false);
                comandaRepository.save(comanda);
            }
        }
    }

    // Arquiva a comanda como fiado vinculando o CPF ou telefone do devedor
    @Transactional
    public void archiveAsCredit(Integer comandaId, String customerDocOrPhone) {
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        if (comanda.getStatus() != ComandaStatus.OPEN) {
            throw new BusinessRuleException("Apenas comandas abertas podem ser arquivadas como fiado.");
        }
        BigDecimal balance = comanda.getTotalAmount().subtract(comanda.getPaidAmount());
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Não é possível arquivar como fiado uma comanda sem saldo devedor.");
        }
        if (customerDocOrPhone == null || customerDocOrPhone.trim().isEmpty()) {
            throw new BusinessRuleException("CPF ou Telefone é obrigatório para registrar pendência.");
        }
        comanda.setCustomerDocOrPhone(customerDocOrPhone.trim());
        comanda.setStatus(ComandaStatus.IN_DEBT);
        comanda.setDeferredDate(LocalDateTime.now());
        comandaRepository.save(comanda);
        for (Table table : comanda.getTables()) {
            releaseTableIfEmpty(table);
        }
    }

    // Abre uma nova comanda de mesa ou balcao validando se o estabelecimento esta aberto
    @Transactional
    public ComandaSummaryDto openTab(ComandaOpenDto dto) {
        if (!storeSettingsService.isStoreOpen()) {
            throw new BusinessRuleException("O bar está fechado! Não é possível abrir novas mesas/comandas.");
        }

        Comanda comanda = new Comanda();
        comanda.setConsumptionType(dto.consumptionType());
        
        if (dto.consumptionType() == ConsumptionType.MESA) {
            Table table = tableRepository.findByNumber(dto.tableNumber())
                    .orElseThrow(() -> new BusinessRuleException("Mesa " + dto.tableNumber() + " não encontrada no salão!"));
            comanda.getTables().add(table);
            
            if (!table.getOccupied()) {
                table.setOccupied(true);
                tableRepository.save(table);
            }
        }

        if (dto.customerName() == null || dto.customerName().trim().isEmpty()) {
            if (dto.consumptionType() == ConsumptionType.MESA) {
                Table table = comanda.getTables().get(0);
                List<Comanda> abertas = comandaRepository.findByTableIdAndStatus(table.getId(), ComandaStatus.OPEN);
                comanda.setCustomerName("Mesa " + dto.tableNumber() + " - Cliente " + (abertas.size() + 1));
            } else {
                comanda.setCustomerName("Cliente Balcão");
            }
        } else {
            comanda.setCustomerName(dto.customerName().trim());
        }

        comanda = comandaRepository.save(comanda);
        List<Integer> tableNums = comanda.getTables().stream().map(Table::getNumber).collect(Collectors.toList());
        return new ComandaSummaryDto(comanda.getId(), comanda.getCustomerName(), tableNums, comanda.getConsumptionType().name(), comanda.getTotalAmount());
    }

    // Registra um pagamento parcial ou total para diminuir o saldo devedor
    @Transactional
    public String registerPayment(Integer comandaId, BigDecimal amountToPay, BigDecimal discount) {
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));

        if (comanda.getStatus() == ComandaStatus.CLOSED) {
            throw new BusinessRuleException("Esta comanda já está totalmente paga e fechada.");
        }

        BigDecimal originalBalance = comanda.getTotalAmount().subtract(comanda.getPaidAmount());

        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
            if (!isAdmin) {
                throw new BusinessRuleException("Apenas o Administrador pode conceder descontos. Chame o gerente.");
            }
            if (discount.compareTo(originalBalance) > 0) {
                throw new BusinessRuleException("O valor do desconto (R$ " + discount + ") não pode ser maior que o saldo devedor atual (R$ " + originalBalance + ").");
            }
            comanda.setTotalAmount(comanda.getTotalAmount().subtract(discount));
        }

        BigDecimal balance = comanda.getTotalAmount().subtract(comanda.getPaidAmount());
        if (amountToPay.compareTo(balance) > 0) {
            throw new BusinessRuleException("O valor do pagamento (R$ " + amountToPay + ") é maior que o saldo devedor atual (R$ " + balance + ").");
        }

        comanda.setPaidAmount(comanda.getPaidAmount().add(amountToPay));
        BigDecimal newBalance = comanda.getTotalAmount().subtract(comanda.getPaidAmount());

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            comanda.setStatus(ComandaStatus.CLOSED);
            comandaRepository.save(comanda);
            for (Table table : comanda.getTables()) {
                releaseTableIfEmpty(table);
            }
            return "Pagamento final recebido com sucesso. A comanda foi fechada!";
        }

        comandaRepository.save(comanda);
        return "Pagamento parcial registrado. Saldo devedor restante: R$ " + newBalance + ".";
    }

    // Abre uma comanda de delivery com endereco e taxa de entrega fixa
    @Transactional
    public ComandaSummaryDto openDeliveryTab(ComandaDeliveryDto dto) {
        Comanda comanda = new Comanda();
        comanda.setCustomerName(dto.customerName().trim());
        comanda.setConsumptionType(ConsumptionType.DELIVERY);
        comanda.setDeliveryAddress(dto.deliveryAddress().trim());
        comanda.setDeliveryFee(DELIVERY_FEE);
        comanda.setTotalAmount(DELIVERY_FEE);
        comanda = comandaRepository.save(comanda);
        return new ComandaSummaryDto(comanda.getId(), comanda.getCustomerName(), new ArrayList<>(), comanda.getConsumptionType().name(), comanda.getTotalAmount());
    }

    // Libera a mesa do salao se nao existirem outras comandas abertas nela
    private void releaseTableIfEmpty(Table table) {
        if (table != null) {
            boolean hasOpenTabs = comandaRepository.existsByTableIdAndStatus(table.getId(), ComandaStatus.OPEN);
            if (!hasOpenTabs) {
                table.setOccupied(false);
                tableRepository.save(table);
            }
        }
    }

    // Cancela e exclui uma comanda sem consumo
    @Transactional
    public void cancelTab(Integer comandaId) {
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        if (comanda.getStatus() == ComandaStatus.CLOSED) {
            throw new BusinessRuleException("Não é possível excluir uma comanda já fechada.");
        }
        if (comanda.getStatus() == ComandaStatus.IN_DEBT) {
            throw new BusinessRuleException("Não é possível excluir uma comanda arquivada como fiado.");
        }
        if (comanda.getTotalAmount().compareTo(BigDecimal.ZERO) > 0 || comanda.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Não é possível excluir uma comanda que já possui consumo ou pagamentos registrados. Cancele os itens primeiro.");
        }
        List<Table> tables = new ArrayList<>(comanda.getTables());
        comanda.getTables().clear();
        comandaRepository.save(comanda);
        comandaRepository.delete(comanda);
        for (Table table : tables) {
            releaseTableIfEmpty(table);
        }
    }

    // Transfere a comanda para outra mesa vazia
    @Transactional
    public String transferTab(Integer comandaId, Integer newTableNumber) {
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        if (comanda.getStatus() != ComandaStatus.OPEN) {
            throw new BusinessRuleException("Apenas comandas abertas podem ser transferidas de mesa.");
        }
        Table newTable = tableRepository.findByNumber(newTableNumber)
                .orElseThrow(() -> new ResourceNotFoundException("A nova mesa informada não existe no salão."));
        if (newTable.getOccupied()) {
            throw new BusinessRuleException("Transferência negada: A mesa " + newTableNumber + " já está ocupada por outros clientes!");
        }
        List<Table> oldTables = new ArrayList<>(comanda.getTables());
        newTable.setOccupied(true);
        tableRepository.save(newTable);
        
        comanda.getTables().clear();
        comanda.getTables().add(newTable);
        comanda.setConsumptionType(ConsumptionType.MESA);
        comandaRepository.save(comanda);
        
        for (Table oldTable : oldTables) {
            releaseTableIfEmpty(oldTable);
        }
        return "Comanda transferida com sucesso para a Mesa " + newTableNumber;
    }

    // Liquida o pagamento de varias comandas ao mesmo tempo e calcula o troco
    @Transactional
    public String payGroupedTabs(List<Integer> comandaIds, BigDecimal amountReceived) {
        if (amountReceived == null || amountReceived.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("O valor recebido deve ser maior que zero.");
        }
        List<Comanda> comandas = comandaRepository.findAllById(comandaIds);
        if (comandas.isEmpty()) {
            throw new BusinessRuleException("Nenhuma comanda selecionada.");
        }
        BigDecimal totalBalance = BigDecimal.ZERO;
        for (Comanda comanda : comandas) {
            if (comanda.getStatus() == ComandaStatus.CLOSED) {
                throw new BusinessRuleException("A comanda ID " + comanda.getId() + " já está fechada e não pode ser agrupada.");
            }
            BigDecimal balance = comanda.getTotalAmount().subtract(comanda.getPaidAmount());
            totalBalance = totalBalance.add(balance);
        }
        if (amountReceived.compareTo(totalBalance) < 0) {
            throw new BusinessRuleException("O valor recebido (R$ " + amountReceived + ") é menor que o saldo das comandas (R$ " + totalBalance + ").");
        }
        BigDecimal change = amountReceived.subtract(totalBalance);
        String changeMsg = change.compareTo(BigDecimal.ZERO) > 0 ? " Troco: R$ " + change + "." : "";
        for (Comanda comanda : comandas) {
            comanda.setPaidAmount(comanda.getTotalAmount()); 
            comanda.setStatus(ComandaStatus.CLOSED);
            comandaRepository.save(comanda);
            for (Table table : comanda.getTables()) {
                releaseTableIfEmpty(table);
            }
        }
        return "Sucesso! O pagamento agrupado de R$ " + totalBalance + " foi realizado e " + comandas.size() + " comandas foram liquidadas e fechadas." + changeMsg;
    }

    // Reabre uma comanda fechada
    @Transactional
    public String reopenTab(Integer comandaId) {
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));

        if (comanda.getStatus() != ComandaStatus.CLOSED) {
            throw new BusinessRuleException("Esta comanda já está aberta ou com dívida.");
        }

        boolean lostTable = false;
        List<Table> tables = new ArrayList<>(comanda.getTables());
        for (Table table : tables) {
            if (table.getOccupied()) {
                lostTable = true;
                comanda.getTables().remove(table);
            } else {
                table.setOccupied(true);
                tableRepository.save(table);
            }
        }

        comanda.setStatus(ComandaStatus.OPEN);
        comandaRepository.save(comanda);

        if (lostTable) {
            return "Atenção: A comanda do cliente '" + comanda.getCustomerName() + "' foi reaberta com sucesso. Nota: Alguma mesa original estava ocupada, então essa associação foi removida.";
        }
        return "Atenção: A comanda do cliente '" + comanda.getCustomerName() + "' foi reaberta com sucesso.";
    }

    // Atualiza os dados cadastrais da comanda como o nome do cliente
    @Transactional
    public ComandaSummaryDto patchTab(Integer comandaId, ComandaUpdateDto dto) {
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));

        if (dto.customerName() != null) {
            comanda.setCustomerName(dto.customerName().trim());
        }

        comanda = comandaRepository.save(comanda);
        List<Integer> tableNums = comanda.getTables().stream().map(Table::getNumber).collect(Collectors.toList());
        return new ComandaSummaryDto(comanda.getId(), comanda.getCustomerName(), tableNums, comanda.getConsumptionType().name(), comanda.getTotalAmount());
    }
}
