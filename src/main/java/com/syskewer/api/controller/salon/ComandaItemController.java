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

import com.syskewer.api.dto.salon.ComandaItemRecordDto;
import com.syskewer.api.service.salon.ComandaItemService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/pedidos")
public class ComandaItemController {

    private final ComandaItemService comandaItemService;

    public ComandaItemController(ComandaItemService comandaItemService) {
        this.comandaItemService = comandaItemService;
    }

    // Lanca um novo pedido em uma comanda aberta
    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody @Valid ComandaItemRecordDto dto) {
        comandaItemService.placeOrder(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Pedido lançado com sucesso, calculado e enviado para a fila!");
    }

    // Cancela um item especifico de um pedido
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<Void> cancelOrderItem(@PathVariable Long itemId) {
        comandaItemService.cancelOrderItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/items/{itemId}/reduce")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> reduceQuantity(@PathVariable Long itemId, @RequestParam Integer quantityToRemove) {
        comandaItemService.reduceItemQuantity(itemId, quantityToRemove);
        return ResponseEntity.ok("Quantidade reduzida e conta atualizada.");
    }

    @PostMapping("/items/{itemId}/split")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> splitItem(@PathVariable Long itemId, @RequestBody List<Integer> targetComandaIds) {
        comandaItemService.splitItem(itemId, targetComandaIds);
        return ResponseEntity.ok("O item foi rachado com sucesso entre as comandas.");
    }
}
