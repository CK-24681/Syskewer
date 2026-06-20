package com.syskewer.api.service.salon;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.syskewer.api.dto.salon.KitchenItemDto;
import com.syskewer.api.dto.salon.KitchenOrderDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.OrderDetail;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.repository.salon.OrderDetailRepository;
import com.syskewer.api.repository.salon.OrderRepository;

@Service
public class KitchenService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    public KitchenService(OrderRepository orderRepository, OrderDetailRepository orderDetailRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
    }

    // Retorna a fila de pedidos ativos com base na praca de preparo
    public List<KitchenOrderDto> getKitchenQueue(String location) {
        List<PrepStatus> activeStatuses = Arrays.asList(PrepStatus.QUEUED, PrepStatus.PREPARING);
        List<Order> orders = orderRepository.findByPrepStatusInOrderByCreatedAtAsc(activeStatuses);

        return orders.stream().map(order -> {
            String destination;
            if (!order.getBill().getTables().isEmpty()) {
                destination = "Mesa " + order.getBill().getTables().stream()
                        .map(t -> String.valueOf(t.getNumber()))
                        .collect(Collectors.joining(", "));
            } else {
                destination = "Tab " + order.getBill().getId() + " (" + order.getBill().getCustomerName() + ")";
            }

            // Identifica quem pediu
            String waiterName = order.getWaiter() != null ? order.getWaiter().getName() : "Autoatendimento/Delivery";

            List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());

            // Filtro de Roteamento Inteligente (Cozinha vs Churrasqueira)
            if (location != null && !location.isBlank()) {
                details = details.stream()
                        .filter(item -> item.getMenu().getPrepLocation() != null &&
                                item.getMenu().getPrepLocation().getName().equalsIgnoreCase(location))
                        .collect(Collectors.toList());
            }

            // Se o pedido não tiver itens para esta praça de preparo, não exibe o card
            if (details.isEmpty()) {
                return null;
            }

            List<KitchenItemDto> items = details.stream().map(item -> new KitchenItemDto(
                    item.getMenu().getName(),
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
                    waiterName,
                    items
            );
        })
        .filter(java.util.Objects::nonNull) // Remove os nulos do roteamento
        .collect(Collectors.toList());
    }

    public void advanceOrderStatus(Long orderId, PrepStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found in the kitchen!"));

        if (newStatus == null) {
            throw new BusinessRuleException("New status cannot be null.");
        }

        if (order.getPrepStatus().ordinal() > newStatus.ordinal()) {
            throw new BusinessRuleException("Not allowed to revert prep status from " 
                    + order.getPrepStatus() + " to " + newStatus + ".");
        }

        order.setPrepStatus(newStatus);
        orderRepository.save(order);
    }
}