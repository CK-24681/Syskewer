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
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Launch a new order in an open bill
    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody @Valid OrderRecordDto dto) {
        orderService.placeOrder(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Order placed successfully, calculated and sent to queue!");
    }

    // Cancel a specific item of an order
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
        return ResponseEntity.ok("Quantity reduced and bill updated.");
    }

    @PostMapping("/items/{itemId}/split")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> splitItem(@PathVariable Long itemId, @RequestBody List<Integer> targetBillIds) {
        orderService.splitItem(itemId, targetBillIds);
        return ResponseEntity.ok("The item was successfully split among the bills.");
    }
}
