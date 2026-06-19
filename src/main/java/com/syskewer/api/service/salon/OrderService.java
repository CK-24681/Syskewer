package com.syskewer.api.service.salon;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syskewer.api.dto.salon.OrderItemRecordDto;
import com.syskewer.api.dto.salon.OrderRecordDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
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

// Servico para gerenciar os pedidos e itens de consumo
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TabRepository tabRepository;
    private final ProductRepository productRepository;
    private final StoreSettingsService storeSettingsService;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
            TabRepository tabRepository, ProductRepository productRepository,
            StoreSettingsService storeSettingsService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.tabRepository = tabRepository;
        this.productRepository = productRepository;
        this.storeSettingsService = storeSettingsService;
    }

    // Registra um novo pedido com os itens desejados na comanda
    @Transactional
    public void placeOrder(OrderRecordDto dto) {
        if (!storeSettingsService.isStoreOpen()) {
            throw new BusinessRuleException("O bar está fechado! Lançamento de pedidos suspenso.");
        }
        Tab tab = tabRepository.findById(dto.tabId())
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada!"));
        
        if (tab.getStatus() == TabStatus.CLOSED || tab.getStatus() == TabStatus.IN_DEBT) {
            throw new BusinessRuleException(
                    "Não é possível lançar pedidos em uma comanda fechada ou com dívida pendente (Fiado).");
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User waiter = null;
        if (principal instanceof User user) {
            waiter = user;
        }

        Order order = new Order();
        order.setTab(tab);
        order.setWaiter(waiter);
        order.setOrigin(dto.origin());
        order.setPrepStatus(PrepStatus.QUEUED);
        order = orderRepository.save(order);

        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        for (OrderItemRecordDto itemDto : dto.items()) {
            Product product = productRepository.findById(itemDto.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produto ID " + itemDto.productId() + " não encontrado no cardápio!"));
            
            if (Boolean.FALSE.equals(product.getActive())) {
                throw new BusinessRuleException("O produto '" + product.getName() + "' foi desativado e não pode ser vendido.");
            }
            if (Boolean.FALSE.equals(product.getInStock())) {
                throw new BusinessRuleException("O produto '" + product.getName() + "' está fora de estoque no momento.");
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemDto.quantity());
            item.setSoldPrice(product.getPrice());
            item.setIsToGo(Boolean.TRUE.equals(itemDto.isToGo()));
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

    // Cancela um item de um pedido e atualiza o total da comanda
    @Transactional
    public void cancelOrderItem(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado!"));
        Order order = item.getOrder();
        Tab tab = order.getTab();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));

        if (order.getPrepStatus() != PrepStatus.QUEUED && !isAdmin) {
            throw new BusinessRuleException("Apenas itens com status 'QUEUED' (Na fila) podem ser cancelados pelo Garçom. Chame o gerente para estornar um prato que já está sendo feito ou entregue.");
        }

        if (tab.getStatus() == TabStatus.CLOSED && !isAdmin) {
            throw new BusinessRuleException("A comanda já está fechada. Apenas o gerente pode estornar um item de conta fechada.");
        }

        BigDecimal itemTotal = item.getSoldPrice().multiply(new BigDecimal(item.getQuantity()));
        
        // Se altera a comanda, subtrai do total gasto
        tab.setTotalAmount(tab.getTotalAmount().subtract(itemTotal));

        // A MÁGICA DA SUA OBSERVAÇÃO #3: Se o gerente estornar um item de conta FECHADA, 
        // a gente abate do Valor Pago também para simular que você devolveu o dinheiro para ele.
        if (tab.getStatus() == TabStatus.CLOSED) {
            tab.setPaidAmount(tab.getPaidAmount().subtract(itemTotal));
        }

        tabRepository.save(tab);
        orderItemRepository.delete(item);
    }

    // Reduz a quantidade de um item especifico do pedido
    @Transactional
    public void reduceItemQuantity(Long itemId, Integer quantityToRemove) {
        if (quantityToRemove == null || quantityToRemove <= 0) {
            throw new BusinessRuleException("A quantidade a ser removida deve ser maior que zero.");
        }

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado!"));
        
        Order order = item.getOrder();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));

        if (order.getPrepStatus() != PrepStatus.QUEUED && !isAdmin) {
            throw new BusinessRuleException("O preparo já começou. Apenas o gerente pode diminuir a quantidade agora.");
        }

        if (quantityToRemove >= item.getQuantity()) {
            throw new BusinessRuleException("Para remover todos os itens, utilize a função de cancelar o item inteiro.");
        }

        // Calcula o estorno e abate da comanda
        BigDecimal refundAmount = item.getSoldPrice().multiply(new BigDecimal(quantityToRemove));
        Tab tab = order.getTab();
        tab.setTotalAmount(tab.getTotalAmount().subtract(refundAmount));
        
        if (tab.getStatus() == TabStatus.CLOSED) {
            tab.setPaidAmount(tab.getPaidAmount().subtract(refundAmount));
        }
        tabRepository.save(tab);

        // Atualiza a quantidade fisicamente
        item.setQuantity(item.getQuantity() - quantityToRemove);
        orderItemRepository.save(item);
    }

    // Divide o valor de um item de consumo entre varias comandas
    @Transactional
    public void splitItem(Long itemId, List<Integer> targetTabIds) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado!"));
        
        if (targetTabIds.isEmpty()) {
            throw new BusinessRuleException("Nenhuma comanda de destino selecionada para o racha.");
        }

        Tab originalTab = item.getOrder().getTab();
        int totalPeople = targetTabIds.size() + 1;

        BigDecimal itemTotal = item.getSoldPrice().multiply(new BigDecimal(item.getQuantity()));
        BigDecimal splitValue = itemTotal.divide(new BigDecimal(totalPeople), 2, java.math.RoundingMode.HALF_UP);

        BigDecimal totalDeduction = splitValue.multiply(new BigDecimal(targetTabIds.size()));
        
        // DEDUZ DO TOTAL DA COMANDA
        originalTab.setTotalAmount(originalTab.getTotalAmount().subtract(totalDeduction));
        tabRepository.save(originalTab);

        // CORREÇÃO: Cria um item de "Desconto de Racha" na comanda original para a conta fechar!
        Order financialOrderOriginal = new Order();
        financialOrderOriginal.setTab(originalTab);
        financialOrderOriginal.setOrigin(OrderOrigin.WAITER);
        financialOrderOriginal.setPrepStatus(PrepStatus.DELIVERED);
        financialOrderOriginal = orderRepository.save(financialOrderOriginal);

        OrderItem discountPart = new OrderItem();
        discountPart.setOrder(financialOrderOriginal);
        discountPart.setProduct(item.getProduct());
        discountPart.setQuantity(1);
        discountPart.setSoldPrice(totalDeduction.negate()); // VALOR NEGATIVO
        discountPart.setNotes("Abatimento: Item rachado com " + targetTabIds.size() + " amigo(s)");
        orderItemRepository.save(discountPart);

        // Distribui a dívida para os amigos
        for (Integer targetId : targetTabIds) {
            Tab targetTab = tabRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessRuleException("Comanda de destino ID " + targetId + " não encontrada."));
            
            if (targetTab.getStatus() == TabStatus.CLOSED) {
                throw new BusinessRuleException("A comanda " + targetTab.getCustomerName() + " já está fechada e não pode entrar no racha.");
            }

            Order financialOrder = new Order();
            financialOrder.setTab(targetTab);
            financialOrder.setOrigin(OrderOrigin.WAITER);
            financialOrder.setPrepStatus(PrepStatus.DELIVERED);
            financialOrder = orderRepository.save(financialOrder);

            OrderItem splitPart = new OrderItem();
            splitPart.setOrder(financialOrder);
            splitPart.setProduct(item.getProduct());
            splitPart.setQuantity(1);
            splitPart.setSoldPrice(splitValue);
            splitPart.setNotes("Fração dividida: " + item.getProduct().getName());
            orderItemRepository.save(splitPart);

            targetTab.setTotalAmount(targetTab.getTotalAmount().add(splitValue));
            tabRepository.save(targetTab);
        }
    }
}