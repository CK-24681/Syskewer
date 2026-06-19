package com.syskewer.api.service.salon;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.syskewer.api.dto.salon.KitchenItemDto;
import com.syskewer.api.dto.salon.KitchenComandaItemDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.salon.ComandaItem;
import com.syskewer.api.model.salon.ComandaItemDetail;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.repository.salon.ComandaItemDetailRepository;
import com.syskewer.api.repository.salon.ComandaItemRepository;

@Service
public class KitchenService {

    private final ComandaItemRepository comandaItemRepository;
    private final ComandaItemDetailRepository comandaItemDetailRepository;

    public KitchenService(ComandaItemRepository comandaItemRepository, ComandaItemDetailRepository comandaItemDetailRepository) {
        this.comandaItemRepository = comandaItemRepository;
        this.comandaItemDetailRepository = comandaItemDetailRepository;
    }

    // Retorna a fila de pedidos ativos com base na praca de preparo
    public List<KitchenComandaItemDto> getKitchenQueue(String location) {
        List<PrepStatus> activeStatuses = Arrays.asList(PrepStatus.QUEUED, PrepStatus.PREPARING);
        List<ComandaItem> itemsComanda = comandaItemRepository.findByPrepStatusInOrderByCreatedAtAsc(activeStatuses);

        return itemsComanda.stream().map(comandaItem -> {
            String destination;
            if (!comandaItem.getComanda().getTables().isEmpty()) {
                destination = "Mesa " + comandaItem.getComanda().getTables().stream()
                        .map(t -> String.valueOf(t.getNumber()))
                        .collect(Collectors.joining(", "));
            } else {
                destination = "Comanda " + comandaItem.getComanda().getId() + " (" + comandaItem.getComanda().getCustomerName() + ")";
            }

            // Identifica quem pediu
            String waiterName = comandaItem.getWaiter() != null ? comandaItem.getWaiter().getName() : "Autoatendimento/Delivery";

            List<ComandaItemDetail> details = comandaItemDetailRepository.findByComandaItemId(comandaItem.getId());

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

            return new KitchenComandaItemDto(
                    comandaItem.getId(),
                    comandaItem.getOrigin().name(),
                    destination,
                    comandaItem.getPrepStatus().name(),
                    comandaItem.getCreatedAt(),
                    waiterName,
                    items
            );
        })
        .filter(java.util.Objects::nonNull) // Remove os nulos do roteamento
        .collect(Collectors.toList());
    }

    public void advanceOrderStatus(Long orderId, PrepStatus newStatus) {
        ComandaItem comandaItem = comandaItemRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido " + orderId + " não encontrado na cozinha!"));

        if (newStatus == null) {
            throw new BusinessRuleException("O novo status não pode ser nulo.");
        }

        if (comandaItem.getPrepStatus().ordinal() > newStatus.ordinal()) {
            throw new BusinessRuleException("Não é permitido retroceder o status de preparo do pedido de " 
                    + comandaItem.getPrepStatus() + " para " + newStatus + ".");
        }

        comandaItem.setPrepStatus(newStatus);
        comandaItemRepository.save(comandaItem);
    }
}