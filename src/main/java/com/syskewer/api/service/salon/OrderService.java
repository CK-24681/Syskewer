package com.syskewer.api.service.salon;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syskewer.api.dto.salon.OrderDetailRecordDto;
import com.syskewer.api.dto.salon.OrderRecordDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.product.Menu;
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderDetail;
import com.syskewer.api.model.salon.OrderOrigin;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.model.salon.Bill;
import com.syskewer.api.model.salon.BillStatus;
import com.syskewer.api.model.user.User;
import com.syskewer.api.repository.product.MenuRepository;
import com.syskewer.api.repository.salon.OrderDetailRepository;
import com.syskewer.api.repository.salon.OrderRepository;
import com.syskewer.api.repository.salon.BillRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final BillRepository billRepository;
    private final MenuRepository menuRepository;
    private final StoreSettingsService storeSettingsService;

    public OrderService(OrderRepository orderRepository, OrderDetailRepository orderDetailRepository,
            BillRepository billRepository, MenuRepository menuRepository,
            StoreSettingsService storeSettingsService) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.billRepository = billRepository;
        this.menuRepository = menuRepository;
        this.storeSettingsService = storeSettingsService;
    }

    // Launch a new order ticket on a bill
    @Transactional
    public void placeOrder(OrderRecordDto dto) {
        if (!storeSettingsService.isStoreOpen()) {
            throw new BusinessRuleException("The store is closed! Order placement is suspended.");
        }
        Bill bill = billRepository.findById(dto.billId())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found!"));
        
        if (bill.getStatus() == BillStatus.CLOSED || bill.getStatus() == BillStatus.IN_DEBT) {
            throw new BusinessRuleException(
                    "Cannot place orders on a closed bill or a bill marked as deferred debt.");
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User waiter = null;
        if (principal instanceof User user) {
            waiter = user;
        }

        Order order = new Order();
        order.setBill(bill);
        order.setWaiter(waiter);
        order.setOrigin(dto.origin());
        order.setPrepStatus(PrepStatus.QUEUED);
        order = orderRepository.save(order);

        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        for (OrderDetailRecordDto itemDto : dto.items()) {
            Menu menu = menuRepository.findById(itemDto.menuId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product ID " + itemDto.menuId() + " not found in the menu!"));
            
            if (Boolean.FALSE.equals(menu.getActive())) {
                throw new BusinessRuleException("The product '" + menu.getName() + "' is inactive and cannot be sold.");
            }

            // A menu item is in stock if all its physical ingredients are in stock
            boolean inStock = menu.getProducts().isEmpty() || menu.getProducts().stream().allMatch(p -> Boolean.TRUE.equals(p.getInStock()));
            if (!inStock) {
                throw new BusinessRuleException("The product '" + menu.getName() + "' is currently out of stock.");
            }

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setMenu(menu);
            detail.setQuantity(itemDto.quantity());
            detail.setSoldPrice(menu.getPrice());
            detail.setIsToGo(Boolean.TRUE.equals(itemDto.isToGo()));
            detail.setPackagingInstructions(itemDto.packagingInstructions());
            detail.setNotes(itemDto.notes());
            if (itemDto.sideDishes() != null) {
                detail.setSideDishes(itemDto.sideDishes());
            }

            orderDetailRepository.save(detail);
            BigDecimal itemTotal = menu.getPrice().multiply(new BigDecimal(itemDto.quantity()));
            totalOrderAmount = totalOrderAmount.add(itemTotal);
        }

        bill.setTotalAmount(bill.getTotalAmount().add(totalOrderAmount));
        billRepository.save(bill);
    }

    // Cancel an item from an order and update the bill total
    @Transactional
    public void cancelOrderItem(Long itemId) {
        OrderDetail detail = orderDetailRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found!"));
        Order order = detail.getOrder();
        Bill bill = order.getBill();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));

        if (order.getPrepStatus() != PrepStatus.QUEUED && !isAdmin) {
            throw new BusinessRuleException("Only items with 'QUEUED' status can be canceled by waiters. Call manager to refund item currently in prep or delivered.");
        }

        if (bill.getStatus() == BillStatus.CLOSED && !isAdmin) {
            throw new BusinessRuleException("The bill is closed. Only managers can refund items from a closed bill.");
        }

        BigDecimal itemTotal = detail.getSoldPrice().multiply(new BigDecimal(detail.getQuantity()));
        
        bill.setTotalAmount(bill.getTotalAmount().subtract(itemTotal));

        if (bill.getStatus() == BillStatus.CLOSED) {
            bill.setPaidAmount(bill.getPaidAmount().subtract(itemTotal));
        }

        billRepository.save(bill);
        orderDetailRepository.delete(detail);
    }

    // Reduce quantity of a specific item from the order
    @Transactional
    public void reduceItemQuantity(Long itemId, Integer quantityToRemove) {
        if (quantityToRemove == null || quantityToRemove <= 0) {
            throw new BusinessRuleException("Quantity to remove must be greater than zero.");
        }

        OrderDetail detail = orderDetailRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found!"));
        
        Order order = detail.getOrder();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));

        if (order.getPrepStatus() != PrepStatus.QUEUED && !isAdmin) {
            throw new BusinessRuleException("Preparation already started. Only managers can decrease quantity now.");
        }

        if (quantityToRemove >= detail.getQuantity()) {
            throw new BusinessRuleException("To remove all items, use the cancel item function.");
        }

        BigDecimal refundAmount = detail.getSoldPrice().multiply(new BigDecimal(quantityToRemove));
        Bill bill = order.getBill();
        bill.setTotalAmount(bill.getTotalAmount().subtract(refundAmount));
        
        if (bill.getStatus() == BillStatus.CLOSED) {
            bill.setPaidAmount(bill.getPaidAmount().subtract(refundAmount));
        }
        billRepository.save(bill);

        detail.setQuantity(detail.getQuantity() - quantityToRemove);
        orderDetailRepository.save(detail);
    }

    // Split the cost of an item among multiple bills
    @Transactional
    public void splitItem(Long itemId, List<Integer> targetBillIds) {
        OrderDetail detail = orderDetailRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found!"));
        
        if (targetBillIds.isEmpty()) {
            throw new BusinessRuleException("No target bills selected for splitting.");
        }

        Bill originalBill = detail.getOrder().getBill();
        int totalPeople = targetBillIds.size() + 1;

        BigDecimal itemTotal = detail.getSoldPrice().multiply(new BigDecimal(detail.getQuantity()));
        BigDecimal splitValue = itemTotal.divide(new BigDecimal(totalPeople), 2, java.math.RoundingMode.HALF_UP);

        BigDecimal totalDeduction = splitValue.multiply(new BigDecimal(targetBillIds.size()));
        
        originalBill.setTotalAmount(originalBill.getTotalAmount().subtract(totalDeduction));
        billRepository.save(originalBill);

        Order financialOrderOriginal = new Order();
        financialOrderOriginal.setBill(originalBill);
        financialOrderOriginal.setOrigin(OrderOrigin.WAITER);
        financialOrderOriginal.setPrepStatus(PrepStatus.DELIVERED);
        financialOrderOriginal = orderRepository.save(financialOrderOriginal);

        OrderDetail discountPart = new OrderDetail();
        discountPart.setOrder(financialOrderOriginal);
        discountPart.setMenu(detail.getMenu());
        discountPart.setQuantity(1);
        discountPart.setSoldPrice(totalDeduction.negate()); 
        discountPart.setNotes("Split discount: Item split with " + targetBillIds.size() + " friend(s)");
        orderDetailRepository.save(discountPart);

        for (Integer targetId : targetBillIds) {
            Bill targetBill = billRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessRuleException("Target bill ID " + targetId + " not found."));
            
            if (targetBill.getStatus() == BillStatus.CLOSED) {
                throw new BusinessRuleException("Target bill " + targetBill.getCustomerName() + " is already closed and cannot be part of the split.");
            }

            Order financialOrder = new Order();
            financialOrder.setBill(targetBill);
            financialOrder.setOrigin(OrderOrigin.WAITER);
            financialOrder.setPrepStatus(PrepStatus.DELIVERED);
            financialOrder = orderRepository.save(financialOrder);

            OrderDetail splitPart = new OrderDetail();
            splitPart.setOrder(financialOrder);
            splitPart.setMenu(detail.getMenu());
            splitPart.setQuantity(1);
            splitPart.setSoldPrice(splitValue);
            splitPart.setNotes("Split fraction: " + detail.getMenu().getName());
            orderDetailRepository.save(splitPart);

            targetBill.setTotalAmount(targetBill.getTotalAmount().add(splitValue));
            billRepository.save(targetBill);
        }
    }
}
