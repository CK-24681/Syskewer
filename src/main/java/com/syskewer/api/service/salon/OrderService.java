package com.syskewer.api.service.salon;

import java.math.BigDecimal;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syskewer.api.dto.salon.OrderItemRecordDto;
import com.syskewer.api.dto.salon.OrderRecordDto;
import com.syskewer.api.model.product.Product;
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderItem;
import com.syskewer.api.model.salon.OrderOrigin;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.model.salon.Tab;
import com.syskewer.api.model.salon.TabStatus;
import com.syskewer.api.model.user.User;
import com.syskewer.api.repository.product.ProductRepository;
import com.syskewer.api.repository.salon.OrderItemRepository;
import com.syskewer.api.repository.salon.OrderRepository;
import com.syskewer.api.repository.salon.TabRepository;

/** Lança pedidos na comanda e congela o preço do momento da venda. */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TabRepository tabRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        TabRepository tabRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.tabRepository = tabRepository;
        this.productRepository = productRepository;
    }

    /**
     * @param dto comanda, itens e origem do pedido
     */
    @Transactional
    public void placeOrder(OrderRecordDto dto) {

        Tab tab = tabRepository.findById(dto.tabId())
                .orElseThrow(() -> new RuntimeException("Comanda não encontrada!"));

        if (tab.getStatus() == TabStatus.CLOSED) {
            throw new RuntimeException("Não é possível lançar pedidos em uma comanda fechada.");
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User waiter = null;
        if (principal instanceof User user) {
            waiter = user;
        }

        Order order = new Order();
        order.setTab(tab);
        order.setWaiter(waiter);
        order.setOrigin(dto.origin() != null ? dto.origin() : OrderOrigin.WAITER);
        order.setPrepStatus(PrepStatus.QUEUED);

        order = orderRepository.save(order);

        BigDecimal totalOrderAmount = BigDecimal.ZERO;

        for (OrderItemRecordDto itemDto : dto.items()) {
            Product product = productRepository.findById(itemDto.productId())
                    .orElseThrow(() -> new RuntimeException("Produto ID " + itemDto.productId() + " não encontrado!"));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemDto.quantity());
            item.setSoldPrice(product.getPrice());

            Boolean isToGo = itemDto.isToGo();
            item.setIsToGo(Boolean.TRUE.equals(isToGo));
            item.setPackagingInstructions(itemDto.packagingInstructions());
            item.setNotes(itemDto.notes());

            if (itemDto.sideDishes() != null) {
                item.setSideDishes(itemDto.sideDishes());
            }

            orderItemRepository.save(item);

            BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(itemDto.quantity()));
            totalOrderAmount = totalOrderAmount.add(itemTotal);
        }

        tab.setTotalAmount(tab.getTotalAmount().add(totalOrderAmount));
        tabRepository.save(tab);
    }
}
