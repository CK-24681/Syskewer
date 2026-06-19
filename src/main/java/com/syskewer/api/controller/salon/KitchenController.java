package com.syskewer.api.controller.salon;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syskewer.api.dto.salon.KitchenComandaItemDto;
import com.syskewer.api.model.salon.PrepStatus;
import com.syskewer.api.service.salon.KitchenService;

@RestController
@RequestMapping("/kitchen")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    // Retorna a fila de pedidos pendentes para a cozinha
    @GetMapping("/queue")
    public ResponseEntity<List<KitchenComandaItemDto>> getQueue(@RequestParam(required = false) String location) {
        return ResponseEntity.ok(kitchenService.getKitchenQueue(location));
    }

    // Atualiza o status de preparo do pedido
    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<String> updateStatus(@PathVariable Long id, @RequestParam PrepStatus status) {
        kitchenService.advanceOrderStatus(id, status);
        return ResponseEntity.ok("Status atualizado para: " + status);
    }
}
