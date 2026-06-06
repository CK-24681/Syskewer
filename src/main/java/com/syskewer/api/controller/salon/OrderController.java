package com.syskewer.api.controller.salon;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
