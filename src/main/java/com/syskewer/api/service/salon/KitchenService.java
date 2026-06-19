package com.syskewer.api.service.salon;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.syskewer.api.dto.salon.KitchenItemDto;
import com.syskewer.api.dto.salon.KitchenOrderDto;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderItem;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.repository.salon.OrderItemRepository;
import com.syskewer.api.repository.salon.OrderRepository;

@Service
public class KitchenService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public KitchenService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public List<KitchenOrderDto> getKitchenQueue(String location) {
        List<PrepStatus> activeStatuses = Arrays.asList(PrepStatus.QUEUED, PrepStatus.PREPARING);
        List<Order> orders = orderRepository.findByPrepStatusInOrderByCreatedAtAsc(activeStatuses);

        return orders.stream().map(order -> {
            String destination = order.getTab().getTable() != null
                    ? "Mesa " + order.getTab().getTable().getNumber()
                    : "Comanda " + order.getTab().getId() + " (" + order.getTab().getCustomerName() + ")";

            // Identifica quem pediu
            String waiterName = order.getWaiter() != null ? order.getWaiter().getName() : "Autoatendimento/Delivery";

            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

            // Filtro de Roteamento Inteligente (Cozinha vs Churrasqueira)
            if (location != null && !location.isBlank()) {
                orderItems = orderItems.stream()
                        .filter(item -> item.getProduct().getPrepLocation() != null &&
                                item.getProduct().getPrepLocation().getName().equalsIgnoreCase(location))
                        .collect(Collectors.toList());
            }

            // Se o pedido não tiver itens para esta praça de preparo, não exibe o card
            if (orderItems.isEmpty()) {
                return null;
            }

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
                    waiterName, // <-- Injetado aqui
                    items
            );
        })
        .filter(java.util.Objects::nonNull) // Remove os nulos do roteamento
        .collect(Collectors.toList());
    }

    public void advanceOrderStatus(Long orderId, PrepStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido " + orderId + " não encontrado na cozinha!"));

        order.setPrepStatus(newStatus);
        orderRepository.save(order);
    }
}