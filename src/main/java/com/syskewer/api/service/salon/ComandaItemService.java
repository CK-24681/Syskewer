package com.syskewer.api.service.salon;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syskewer.api.dto.salon.ComandaItemDetailRecordDto;
import com.syskewer.api.dto.salon.ComandaItemRecordDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.product.Menu;
import com.syskewer.api.model.salon.ComandaItem;
import com.syskewer.api.model.salon.ComandaItemDetail;
import com.syskewer.api.model.salon.OrderOrigin;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.model.salon.Comanda;
import com.syskewer.api.model.salon.ComandaStatus;
import com.syskewer.api.model.user.User;
import com.syskewer.api.repository.product.MenuRepository;
import com.syskewer.api.repository.salon.ComandaItemDetailRepository;
import com.syskewer.api.repository.salon.ComandaItemRepository;
import com.syskewer.api.repository.salon.ComandaRepository;

@Service
public class ComandaItemService {

    private final ComandaItemRepository comandaItemRepository;
    private final ComandaItemDetailRepository comandaItemDetailRepository;
    private final ComandaRepository comandaRepository;
    private final MenuRepository menuRepository;
    private final StoreSettingsService storeSettingsService;

    public ComandaItemService(ComandaItemRepository comandaItemRepository, ComandaItemDetailRepository comandaItemDetailRepository,
            ComandaRepository comandaRepository, MenuRepository menuRepository,
            StoreSettingsService storeSettingsService) {
        this.comandaItemRepository = comandaItemRepository;
        this.comandaItemDetailRepository = comandaItemDetailRepository;
        this.comandaRepository = comandaRepository;
        this.menuRepository = menuRepository;
        this.storeSettingsService = storeSettingsService;
    }

    // Registra um novo pedido com os itens desejados na comanda
    @Transactional
    public void placeOrder(ComandaItemRecordDto dto) {
        if (!storeSettingsService.isStoreOpen()) {
            throw new BusinessRuleException("O bar está fechado! Lançamento de pedidos suspenso.");
        }
        Comanda comanda = comandaRepository.findById(dto.comandaId())
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        
        if (comanda.getStatus() == ComandaStatus.CLOSED || comanda.getStatus() == ComandaStatus.IN_DEBT) {
            throw new BusinessRuleException(
                    "Não é possível lançar pedidos em uma comanda fechada ou com dívida pendente (Fiado).");
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User waiter = null;
        if (principal instanceof User user) {
            waiter = user;
        }

        ComandaItem comandaItem = new ComandaItem();
        comandaItem.setComanda(comanda);
        comandaItem.setWaiter(waiter);
        comandaItem.setOrigin(dto.origin());
        comandaItem.setPrepStatus(PrepStatus.QUEUED);
        comandaItem = comandaItemRepository.save(comandaItem);

        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        for (ComandaItemDetailRecordDto itemDto : dto.items()) {
            Menu menu = menuRepository.findById(itemDto.menuId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produto ID " + itemDto.menuId() + " não encontrado no cardápio!"));
            
            if (Boolean.FALSE.equals(menu.getActive())) {
                throw new BusinessRuleException("O produto '" + menu.getName() + "' foi desativado e não pode ser vendido.");
            }

            // Um item do cardápio está em estoque se todos os seus produtos físicos (insumos) estiverem em estoque
            boolean inStock = menu.getProducts().isEmpty() || menu.getProducts().stream().allMatch(p -> Boolean.TRUE.equals(p.getInStock()));
            if (!inStock) {
                throw new BusinessRuleException("O produto '" + menu.getName() + "' está fora de estoque no momento.");
            }

            ComandaItemDetail detail = new ComandaItemDetail();
            detail.setComandaItem(comandaItem);
            detail.setMenu(menu);
            detail.setQuantity(itemDto.quantity());
            detail.setSoldPrice(menu.getPrice());
            detail.setIsToGo(Boolean.TRUE.equals(itemDto.isToGo()));
            detail.setPackagingInstructions(itemDto.packagingInstructions());
            detail.setNotes(itemDto.notes());
            if (itemDto.sideDishes() != null) {
                detail.setSideDishes(itemDto.sideDishes());
            }

            comandaItemDetailRepository.save(detail);
            BigDecimal itemTotal = menu.getPrice().multiply(new BigDecimal(itemDto.quantity()));
            totalOrderAmount = totalOrderAmount.add(itemTotal);
        }

        comanda.setTotalAmount(comanda.getTotalAmount().add(totalOrderAmount));
        comandaRepository.save(comanda);
    }

    // Cancela um item de um pedido e atualiza o total da comanda
    @Transactional
    public void cancelOrderItem(Long itemId) {
        ComandaItemDetail detail = comandaItemDetailRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado!"));
        ComandaItem comandaItem = detail.getComandaItem();
        Comanda comanda = comandaItem.getComanda();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));

        if (comandaItem.getPrepStatus() != PrepStatus.QUEUED && !isAdmin) {
            throw new BusinessRuleException("Apenas itens com status 'QUEUED' (Na fila) podem ser cancelados pelo Garçom. Chame o gerente para estornar um prato que já está sendo feito ou entregue.");
        }

        if (comanda.getStatus() == ComandaStatus.CLOSED && !isAdmin) {
            throw new BusinessRuleException("A comanda já está fechada. Apenas o gerente pode estornar um item de conta fechada.");
        }

        BigDecimal itemTotal = detail.getSoldPrice().multiply(new BigDecimal(detail.getQuantity()));
        
        comanda.setTotalAmount(comanda.getTotalAmount().subtract(itemTotal));

        if (comanda.getStatus() == ComandaStatus.CLOSED) {
            comanda.setPaidAmount(comanda.getPaidAmount().subtract(itemTotal));
        }

        comandaRepository.save(comanda);
        comandaItemDetailRepository.delete(detail);
    }

    // Reduz a quantidade de um item especifico do pedido
    @Transactional
    public void reduceItemQuantity(Long itemId, Integer quantityToRemove) {
        if (quantityToRemove == null || quantityToRemove <= 0) {
            throw new BusinessRuleException("A quantidade a ser removida deve ser maior que zero.");
        }

        ComandaItemDetail detail = comandaItemDetailRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado!"));
        
        ComandaItem comandaItem = detail.getComandaItem();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));

        if (comandaItem.getPrepStatus() != PrepStatus.QUEUED && !isAdmin) {
            throw new BusinessRuleException("O preparo já começou. Apenas o gerente pode diminuir a quantidade agora.");
        }

        if (quantityToRemove >= detail.getQuantity()) {
            throw new BusinessRuleException("Para remover todos os itens, utilize a função de cancelar o item inteiro.");
        }

        BigDecimal refundAmount = detail.getSoldPrice().multiply(new BigDecimal(quantityToRemove));
        Comanda comanda = comandaItem.getComanda();
        comanda.setTotalAmount(comanda.getTotalAmount().subtract(refundAmount));
        
        if (comanda.getStatus() == ComandaStatus.CLOSED) {
            comanda.setPaidAmount(comanda.getPaidAmount().subtract(refundAmount));
        }
        comandaRepository.save(comanda);

        detail.setQuantity(detail.getQuantity() - quantityToRemove);
        comandaItemDetailRepository.save(detail);
    }

    // Divide o valor de um item de consumo entre varias comandas
    @Transactional
    public void splitItem(Long itemId, List<Integer> targetComandaIds) {
        ComandaItemDetail detail = comandaItemDetailRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado!"));
        
        if (targetComandaIds.isEmpty()) {
            throw new BusinessRuleException("Nenhuma comanda de destino selecionada para o racha.");
        }

        Comanda originalComanda = detail.getComandaItem().getComanda();
        int totalPeople = targetComandaIds.size() + 1;

        BigDecimal itemTotal = detail.getSoldPrice().multiply(new BigDecimal(detail.getQuantity()));
        BigDecimal splitValue = itemTotal.divide(new BigDecimal(totalPeople), 2, java.math.RoundingMode.HALF_UP);

        BigDecimal totalDeduction = splitValue.multiply(new BigDecimal(targetComandaIds.size()));
        
        originalComanda.setTotalAmount(originalComanda.getTotalAmount().subtract(totalDeduction));
        comandaRepository.save(originalComanda);

        ComandaItem financialComandaItemOriginal = new ComandaItem();
        financialComandaItemOriginal.setComanda(originalComanda);
        financialComandaItemOriginal.setOrigin(OrderOrigin.WAITER);
        financialComandaItemOriginal.setPrepStatus(PrepStatus.DELIVERED);
        financialComandaItemOriginal = comandaItemRepository.save(financialComandaItemOriginal);

        ComandaItemDetail discountPart = new ComandaItemDetail();
        discountPart.setComandaItem(financialComandaItemOriginal);
        discountPart.setMenu(detail.getMenu());
        discountPart.setQuantity(1);
        discountPart.setSoldPrice(totalDeduction.negate()); 
        discountPart.setNotes("Abatimento: Item rachado com " + targetComandaIds.size() + " amigo(s)");
        comandaItemDetailRepository.save(discountPart);

        for (Integer targetId : targetComandaIds) {
            Comanda targetComanda = comandaRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessRuleException("Comanda de destino ID " + targetId + " não encontrada."));
            
            if (targetComanda.getStatus() == ComandaStatus.CLOSED) {
                throw new BusinessRuleException("A comanda " + targetComanda.getCustomerName() + " já está fechada e não pode entrar no racha.");
            }

            ComandaItem financialComandaItem = new ComandaItem();
            financialComandaItem.setComanda(targetComanda);
            financialComandaItem.setOrigin(OrderOrigin.WAITER);
            financialComandaItem.setPrepStatus(PrepStatus.DELIVERED);
            financialComandaItem = comandaItemRepository.save(financialComandaItem);

            ComandaItemDetail splitPart = new ComandaItemDetail();
            splitPart.setComandaItem(financialComandaItem);
            splitPart.setMenu(detail.getMenu());
            splitPart.setQuantity(1);
            splitPart.setSoldPrice(splitValue);
            splitPart.setNotes("Fração dividida: " + detail.getMenu().getName());
            comandaItemDetailRepository.save(splitPart);

            targetComanda.setTotalAmount(targetComanda.getTotalAmount().add(splitValue));
            comandaRepository.save(targetComanda);
        }
    }
}
