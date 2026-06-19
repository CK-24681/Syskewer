package com.syskewer.api.controller.salon;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syskewer.api.dto.salon.OrderRecordDto;
import com.syskewer.api.service.salon.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * @param dto comanda, itens e observações
     * @return confirmação — pedido vai para a fila da cozinha
     */
    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody @Valid OrderRecordDto dto) {
        orderService.placeOrder(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Pedido lançado com sucesso, calculado e enviado para a fila!");
    }

    /** @param itemId id do item a ser excluído */
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<Void> cancelOrderItem(@PathVariable Long itemId) {
        orderService.cancelOrderItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/items/{itemId}/reduce")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> reduceQuantity(@PathVariable Long itemId, @RequestParam Integer quantityToRemove) {
        orderService.reduceItemQuantity(itemId, quantityToRemove);
        return ResponseEntity.ok("Quantidade reduzida e conta atualizada.");
    }

    @PostMapping("/items/{itemId}/split")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> splitItem(@PathVariable Long itemId, @RequestBody List<Integer> targetTabIds) {
        orderService.splitItem(itemId, targetTabIds);
        return ResponseEntity.ok("O item foi rachado com sucesso entre as comandas.");
    }
}
