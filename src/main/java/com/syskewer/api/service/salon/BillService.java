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
import com.syskewer.api.dto.salon.BillDeliveryDto;
import com.syskewer.api.dto.salon.BillOpenDto;
import com.syskewer.api.dto.salon.BillPartialDto;
import com.syskewer.api.dto.salon.BillSummaryDto;
import com.syskewer.api.dto.salon.BillUpdateDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.salon.ConsumptionType;
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderDetail;
import com.syskewer.api.model.salon.Bill;
import com.syskewer.api.model.salon.BillStatus;
import com.syskewer.api.model.salon.Table;
import com.syskewer.api.repository.salon.OrderDetailRepository;
import com.syskewer.api.repository.salon.OrderRepository;
import com.syskewer.api.repository.salon.BillRepository;
import com.syskewer.api.repository.salon.TableRepository;

@Service
public class BillService {

    private static final BigDecimal COUVERT_VALUE = new BigDecimal("5.00");
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("5.00");

    private final BillRepository billRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final TableRepository tableRepository;
    private final StoreSettingsService storeSettingsService;

    public BillService(BillRepository billRepository, OrderRepository orderRepository,
            OrderDetailRepository orderDetailRepository, TableRepository tableRepository,
            StoreSettingsService storeSettingsService) {
        this.billRepository = billRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.tableRepository = tableRepository;
        this.storeSettingsService = storeSettingsService;
    }

    // Retorna a lista de todas as contas que estao abertas
    public List<BillSummaryDto> getOpenTabs() {
        return billRepository.findByStatus(BillStatus.OPEN).stream().map(bill -> {
            List<Integer> tableNums = bill.getTables().stream().map(Table::getNumber).collect(Collectors.toList());
            return new BillSummaryDto(
                    bill.getId(),
                    bill.getCustomerName(),
                    tableNums,
                    bill.getConsumptionType().name(),
                    bill.getTotalAmount());
        }).collect(Collectors.toList());
    }

    // Calcula e retorna a parcial de consumo e itens da conta
    public BillPartialDto getTabPartial(Integer billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill ID " + billId + " not found"));

        List<Order> orders = orderRepository.findByBillId(billId);
        List<PartialItemDto> timeline = new ArrayList<>();
        Map<String, PartialItemDto> groupedItems = new LinkedHashMap<>();

        for (Order order : orders) {
            List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
            for (OrderDetail detail : details) {
                BigDecimal itemTotalPrice = detail.getSoldPrice().multiply(new BigDecimal(detail.getQuantity()));
                
                PartialItemDto itemDto = new PartialItemDto(
                        detail.getMenu().getName(), detail.getQuantity(), detail.getSoldPrice(), itemTotalPrice,
                        detail.getIsToGo(), order.getPrepStatus().name());

                timeline.add(itemDto);

                String key = detail.getMenu().getName() + (Boolean.TRUE.equals(detail.getIsToGo()) ? " (ToGo)" : "");
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
        return new BillPartialDto(bill.getId(), bill.getCustomerName(), bill.getTotalAmount(), timeline,
                new ArrayList<>(groupedItems.values()));
    }

    // Fecha a conta se o saldo estiver quitado
    @Transactional
    public void closeTab(Integer billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found!"));
        if (bill.getStatus() == BillStatus.CLOSED) {
            throw new BusinessRuleException("This bill is already closed!");
        }
        BigDecimal balance = bill.getTotalAmount().subtract(bill.getPaidAmount());
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("The bill cannot be closed because it has a remaining balance of R$ "
                    + balance + ". Use register payment first.");
        }
        bill.setStatus(BillStatus.CLOSED);
        billRepository.save(bill);
        for (Table table : bill.getTables()) {
            releaseTableIfEmpty(table);
        }
    }

    // Aplica o valor de couvert artistico na conta
    @Transactional
    public void toggleCoverCharge(Integer billId) {
        Bill currentBill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found!"));
        if (currentBill.getStatus() != BillStatus.OPEN) {
            throw new BusinessRuleException("Only open bills can have cover charge modified.");
        }

        if (!currentBill.getTables().isEmpty()) {
            // Se tiver mesas associadas, distribui o couvert entre todas as contas abertas nessas mesas
            List<Bill> tableBills = new ArrayList<>();
            for (Table table : currentBill.getTables()) {
                tableBills.addAll(billRepository.findByTableIdAndStatus(table.getId(), BillStatus.OPEN));
            }
            // Evita duplicatas
            tableBills = tableBills.stream().distinct().collect(Collectors.toList());

            boolean alreadyApplied = tableBills.stream().anyMatch(Bill::getApplyCoverCharge);
            if (alreadyApplied) {
                throw new BusinessRuleException("Cover charge already applied to this table.");
            }
            BigDecimal splitValue = COUVERT_VALUE.divide(new BigDecimal(tableBills.size()), 2, java.math.RoundingMode.HALF_UP);
            for (Bill b : tableBills) {
                b.setApplyCoverCharge(true);
                b.setTotalAmount(b.getTotalAmount().add(splitValue));
                billRepository.save(b);
            }
        } else {
            if (currentBill.getApplyCoverCharge()) {
                throw new BusinessRuleException("Cover charge already applied to this bill.");
            }
            currentBill.setApplyCoverCharge(true);
            currentBill.setTotalAmount(currentBill.getTotalAmount().add(COUVERT_VALUE));
            billRepository.save(currentBill);
        }
    }

    // Remove o valor de couvert artistico da conta
    @Transactional
    public void removeCoverCharge(Integer billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found!"));
        
        if (bill.getStatus() == BillStatus.CLOSED) {
            throw new BusinessRuleException("Closed bill.");
        }

        if (Boolean.TRUE.equals(bill.getApplyCoverCharge()) && !bill.getTables().isEmpty()) {
            List<Bill> tableBills = new ArrayList<>();
            for (Table table : bill.getTables()) {
                tableBills.addAll(billRepository.findByTableIdAndStatus(table.getId(), BillStatus.OPEN));
            }
            tableBills = tableBills.stream().distinct().collect(Collectors.toList());

            long count = tableBills.stream().filter(Bill::getApplyCoverCharge).count();
            if (count > 0) {
                BigDecimal splitValue = COUVERT_VALUE.divide(new BigDecimal(count), 2, java.math.RoundingMode.HALF_UP);
                bill.setTotalAmount(bill.getTotalAmount().subtract(splitValue));
                bill.setApplyCoverCharge(false);
                billRepository.save(bill);
            }
        }
    }

    // Arquiva a conta como fiado vinculando o CPF ou telefone do devedor
    @Transactional
    public void archiveAsCredit(Integer billId, String customerDocOrPhone) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found!"));
        if (bill.getStatus() != BillStatus.OPEN) {
            throw new BusinessRuleException("Only open bills can be archived as deferred debt.");
        }
        BigDecimal balance = bill.getTotalAmount().subtract(bill.getPaidAmount());
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Cannot archive a bill as deferred debt without a remaining balance.");
        }
        if (customerDocOrPhone == null || customerDocOrPhone.trim().isEmpty()) {
            throw new BusinessRuleException("Document or Phone is required to register deferred debt.");
        }
        bill.setCustomerDocOrPhone(customerDocOrPhone.trim());
        bill.setStatus(BillStatus.IN_DEBT);
        bill.setDeferredDate(LocalDateTime.now());
        billRepository.save(bill);
        for (Table table : bill.getTables()) {
            releaseTableIfEmpty(table);
        }
    }

    // Abre uma nova conta de mesa ou balcao validando se o estabelecimento esta aberto
    @Transactional
    public BillSummaryDto openTab(BillOpenDto dto) {
        if (!storeSettingsService.isStoreOpen()) {
            throw new BusinessRuleException("The store is closed! Cannot open new tables or bills.");
        }

        Bill bill = new Bill();
        bill.setConsumptionType(dto.consumptionType());
        
        if (dto.consumptionType() == ConsumptionType.MESA) {
            Table table = tableRepository.findByNumber(dto.tableNumber())
                    .orElseThrow(() -> new BusinessRuleException("Table " + dto.tableNumber() + " not found!"));
            bill.getTables().add(table);
            
            if (!table.getOccupied()) {
                table.setOccupied(true);
                tableRepository.save(table);
            }
        }

        if (dto.customerName() == null || dto.customerName().trim().isEmpty()) {
            if (dto.consumptionType() == ConsumptionType.MESA) {
                Table table = bill.getTables().get(0);
                List<Bill> openBills = billRepository.findByTableIdAndStatus(table.getId(), BillStatus.OPEN);
                bill.setCustomerName("Mesa " + dto.tableNumber() + " - Cliente " + (openBills.size() + 1));
            } else {
                bill.setCustomerName("Cliente Balcão");
            }
        } else {
            bill.setCustomerName(dto.customerName().trim());
        }

        bill = billRepository.save(bill);
        List<Integer> tableNums = bill.getTables().stream().map(Table::getNumber).collect(Collectors.toList());
        return new BillSummaryDto(bill.getId(), bill.getCustomerName(), tableNums, bill.getConsumptionType().name(), bill.getTotalAmount());
    }

    // Registra um pagamento parcial ou total para diminuir o saldo devedor
    @Transactional
    public String registerPayment(Integer billId, BigDecimal amountToPay, BigDecimal discount) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found!"));

        if (bill.getStatus() == BillStatus.CLOSED) {
            throw new BusinessRuleException("This bill is already fully paid and closed.");
        }

        BigDecimal originalBalance = bill.getTotalAmount().subtract(bill.getPaidAmount());

        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
            if (!isAdmin) {
                throw new BusinessRuleException("Only administrators can grant discounts.");
            }
            if (discount.compareTo(originalBalance) > 0) {
                throw new BusinessRuleException("The discount amount (R$ " + discount + ") cannot be greater than the remaining balance (R$ " + originalBalance + ").");
            }
            bill.setTotalAmount(bill.getTotalAmount().subtract(discount));
        }

        BigDecimal balance = bill.getTotalAmount().subtract(bill.getPaidAmount());
        if (amountToPay.compareTo(balance) > 0) {
            throw new BusinessRuleException("The payment amount (R$ " + amountToPay + ") is greater than the remaining balance (R$ " + balance + ").");
        }

        bill.setPaidAmount(bill.getPaidAmount().add(amountToPay));
        BigDecimal newBalance = bill.getTotalAmount().subtract(bill.getPaidAmount());

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(BillStatus.CLOSED);
            billRepository.save(bill);
            for (Table table : bill.getTables()) {
                releaseTableIfEmpty(table);
            }
            return "Final payment received successfully. The bill has been closed!";
        }

        billRepository.save(bill);
        return "Partial payment registered. Remaining balance: R$ " + newBalance + ".";
    }

    // Abre uma conta de delivery com endereco e taxa de entrega fixa
    @Transactional
    public BillSummaryDto openDeliveryTab(BillDeliveryDto dto) {
        Bill bill = new Bill();
        bill.setCustomerName(dto.customerName().trim());
        bill.setConsumptionType(ConsumptionType.DELIVERY);
        bill.setDeliveryAddress(dto.deliveryAddress().trim());
        bill.setDeliveryFee(DELIVERY_FEE);
        bill.setTotalAmount(DELIVERY_FEE);
        bill = billRepository.save(bill);
        return new BillSummaryDto(bill.getId(), bill.getCustomerName(), new ArrayList<>(), bill.getConsumptionType().name(), bill.getTotalAmount());
    }

    // Libera a mesa do salao se nao existirem outras contas abertas nela
    private void releaseTableIfEmpty(Table table) {
        if (table != null) {
            boolean hasOpenTabs = billRepository.existsByTableIdAndStatus(table.getId(), BillStatus.OPEN);
            if (!hasOpenTabs) {
                table.setOccupied(false);
                tableRepository.save(table);
            }
        }
    }

    // Cancela e exclui uma conta sem consumo
    @Transactional
    public void cancelTab(Integer billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found!"));
        if (bill.getStatus() == BillStatus.CLOSED) {
            throw new BusinessRuleException("Cannot delete a closed bill.");
        }
        if (bill.getStatus() == BillStatus.IN_DEBT) {
            throw new BusinessRuleException("Cannot delete a bill archived as deferred debt.");
        }
        if (bill.getTotalAmount().compareTo(BigDecimal.ZERO) > 0 || bill.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Cannot delete a bill with consumption or payments registered. Cancel items first.");
        }
        List<Table> tables = new ArrayList<>(bill.getTables());
        bill.getTables().clear();
        billRepository.save(bill);
        billRepository.delete(bill);
        for (Table table : tables) {
            releaseTableIfEmpty(table);
        }
    }

    // Transfere a conta para outra mesa vazia
    @Transactional
    public String transferTab(Integer billId, Integer newTableNumber) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found!"));
        if (bill.getStatus() != BillStatus.OPEN) {
            throw new BusinessRuleException("Only open bills can be transferred to another table.");
        }
        Table newTable = tableRepository.findByNumber(newTableNumber)
                .orElseThrow(() -> new ResourceNotFoundException("The target table does not exist."));
        if (newTable.getOccupied()) {
            throw new BusinessRuleException("Transfer denied: Table " + newTableNumber + " is already occupied!");
        }
        List<Table> oldTables = new ArrayList<>(bill.getTables());
        newTable.setOccupied(true);
        tableRepository.save(newTable);
        
        bill.getTables().clear();
        bill.getTables().add(newTable);
        bill.setConsumptionType(ConsumptionType.MESA);
        billRepository.save(bill);
        
        for (Table oldTable : oldTables) {
            releaseTableIfEmpty(oldTable);
        }
        return "Bill successfully transferred to Table " + newTableNumber;
    }

    // Liquida o pagamento de varias contas ao mesmo tempo e calcula o troco
    @Transactional
    public String payGroupedTabs(List<Integer> billIds, BigDecimal amountReceived) {
        if (amountReceived == null || amountReceived.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Received amount must be greater than zero.");
        }
        List<Bill> bills = billRepository.findAllById(billIds);
        if (bills.isEmpty()) {
            throw new BusinessRuleException("No bills selected.");
        }
        BigDecimal totalBalance = BigDecimal.ZERO;
        for (Bill bill : bills) {
            if (bill.getStatus() == BillStatus.CLOSED) {
                throw new BusinessRuleException("Bill ID " + bill.getId() + " is already closed and cannot be grouped.");
            }
            BigDecimal balance = bill.getTotalAmount().subtract(bill.getPaidAmount());
            totalBalance = totalBalance.add(balance);
        }
        if (amountReceived.compareTo(totalBalance) < 0) {
            throw new BusinessRuleException("Received amount (R$ " + amountReceived + ") is less than the total balance of the bills (R$ " + totalBalance + ").");
        }
        BigDecimal change = amountReceived.subtract(totalBalance);
        String changeMsg = change.compareTo(BigDecimal.ZERO) > 0 ? " Change: R$ " + change + "." : "";
        for (Bill bill : bills) {
            bill.setPaidAmount(bill.getTotalAmount()); 
            bill.setStatus(BillStatus.CLOSED);
            billRepository.save(bill);
            for (Table table : bill.getTables()) {
                releaseTableIfEmpty(table);
            }
        }
        return "Success! Grouped payment of R$ " + totalBalance + " received, and " + bills.size() + " bills were closed." + changeMsg;
    }

    // Reabre uma conta fechada
    @Transactional
    public String reopenTab(Integer billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found!"));

        if (bill.getStatus() != BillStatus.CLOSED) {
            throw new BusinessRuleException("This bill is already open or marked as deferred debt.");
        }

        boolean lostTable = false;
        List<Table> tables = new ArrayList<>(bill.getTables());
        for (Table table : tables) {
            if (table.getOccupied()) {
                lostTable = true;
                bill.getTables().remove(table);
            } else {
                table.setOccupied(true);
                tableRepository.save(table);
            }
        }

        bill.setStatus(BillStatus.OPEN);
        billRepository.save(bill);

        if (lostTable) {
            return "Attention: Bill for customer '" + bill.getCustomerName() + "' has been reopened. Note: A table was occupied, so its association was removed.";
        }
        return "Attention: Bill for customer '" + bill.getCustomerName() + "' has been reopened.";
    }

    // Atualiza os dados cadastrais da conta como o nome do cliente
    @Transactional
    public BillSummaryDto patchTab(Integer billId, BillUpdateDto dto) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found!"));

        if (dto.customerName() != null) {
            bill.setCustomerName(dto.customerName().trim());
        }

        bill = billRepository.save(bill);
        List<Integer> tableNums = bill.getTables().stream().map(Table::getNumber).collect(Collectors.toList());
        return new BillSummaryDto(bill.getId(), bill.getCustomerName(), tableNums, bill.getConsumptionType().name(), bill.getTotalAmount());
    }
}
