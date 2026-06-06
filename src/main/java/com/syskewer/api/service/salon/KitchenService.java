package com.syskewer.api.service.salon;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.syskewer.api.dto.salon.KitchenItemDto;
import com.syskewer.api.dto.salon.KitchenOrderDto;
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderItem;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.repository.salon.OrderItemRepository;
import com.syskewer.api.repository.salon.OrderRepository;

/** Fila da cozinha — só pedidos em QUEUED ou PREPARING. */
@Service
public class KitchenService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public KitchenService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /** @return pedidos ativos ordenados por chegada */
    public List<KitchenOrderDto> getKitchenQueue() {
        List<PrepStatus> activeStatuses = Arrays.asList(PrepStatus.QUEUED, PrepStatus.PREPARING);
        List<Order> orders = orderRepository.findByPrepStatusInOrderByCreatedAtAsc(activeStatuses);

        return orders.stream().map(order -> {

            String destination = order.getTab().getTable() != null
                    ? "Mesa " + order.getTab().getTable().getNumber()
                    : "Comanda " + order.getTab().getId() + " (" + order.getTab().getCustomerName() + ")";

            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

            List<KitchenItemDto> items = orderItems.stream().map(item -> new KitchenItemDto(
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getIsToGo(),
                    item.getPackagingInstructions(),
                    item.getNotes(),
                    item.getSideDishes()
            )).collect(Collectors.toList());

            return new KitchenOrderDto(
                    order.getId(),
                    order.getOrigin().name(),
                    destination,
                    order.getPrepStatus().name(),
                    order.getCreatedAt(),
                    items
            );
        }).collect(Collectors.toList());
    }

    /**
     * @param orderId id do pedido
     * @param newStatus próximo status (QUEUED → PREPARING → READY)
     */
    public void advanceOrderStatus(Long orderId, PrepStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido " + orderId + " não encontrado!"));

        order.setPrepStatus(newStatus);
        orderRepository.save(order);
    }
}
