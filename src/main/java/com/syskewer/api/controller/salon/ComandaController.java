package com.syskewer.api.controller.salon;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syskewer.api.dto.salon.ComandaPartialDto;
import com.syskewer.api.dto.salon.ComandaPaymentDto;
import com.syskewer.api.dto.salon.ComandaSummaryDto;
import com.syskewer.api.dto.salon.ComandaUpdateDto;
import com.syskewer.api.dto.salon.ComandaOpenDto;
import com.syskewer.api.dto.salon.ComandaDeliveryDto;
import com.syskewer.api.service.salon.ComandaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/comandas")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Comandas", description = "Operações de abertura, pagamento e fechamento de comandas")
public class ComandaController {

    private final ComandaService comandaService;

    public ComandaController(ComandaService comandaService) {
        this.comandaService = comandaService;
    }

    // Lista as comandas abertas no salao
    @GetMapping("/open")
    public ResponseEntity<List<ComandaSummaryDto>> getOpenTabs() {
        return ResponseEntity.ok(comandaService.getOpenTabs());
    }

    // Retorna a parcial de consumo da comanda
    @GetMapping("/{id}/partial")
    public ResponseEntity<ComandaPartialDto> getTabPartial(@PathVariable Integer id) {
        return ResponseEntity.ok(comandaService.getTabPartial(id));
    }

    // Fecha a comanda liberando a mesa
    @PatchMapping("/{id}/close")
    public ResponseEntity<String> closeTab(@PathVariable Integer id) {
        comandaService.closeTab(id);
        return ResponseEntity.ok("Conta recebida com sucesso! A comanda foi fechada e a mesa está livre.");
    }

    // Adiciona o couvert artistico na comanda
    @PatchMapping("/{id}/couvert")
    public ResponseEntity<String> toggleCoverCharge(@PathVariable Integer id) {
        comandaService.toggleCoverCharge(id);
        return ResponseEntity.ok("Couvert artístico processado com sucesso na mesa/comanda!");
    }

    // Abre uma nova comanda de delivery
    @PostMapping("/delivery")
    public ResponseEntity<ComandaSummaryDto> openDeliveryTab(
            @RequestBody @Valid ComandaDeliveryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comandaService.openDeliveryTab(dto));
    }

    // Arquiva a comanda como fiado
    @PatchMapping("/{id}/fiado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<String> archiveAsCredit(@PathVariable Integer id, @RequestParam String doc) {
        comandaService.archiveAsCredit(id, doc);
        return ResponseEntity.ok("Comanda arquivada como Fiado/Inadimplente. Mesa liberada!");
    }

    // Abre uma nova comanda de mesa ou balcao
    @PostMapping
    public ResponseEntity<ComandaSummaryDto> openTab(
            @RequestBody @Valid ComandaOpenDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comandaService.openTab(dto));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<ComandaSummaryDto> patchTab(@PathVariable Integer id, @RequestBody ComandaUpdateDto dto) {
        return ResponseEntity.ok(comandaService.patchTab(id, dto));
    }

    // Registra pagamento parcial ou total da comanda
    @PatchMapping("/{id}/pay")
    public ResponseEntity<String> registerPayment(@PathVariable Integer id, @RequestBody @Valid ComandaPaymentDto dto) {
        String message = comandaService.registerPayment(id, dto.amount(), dto.discount());
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<Void> cancelTab(@PathVariable Integer id) {
        comandaService.cancelTab(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> transferTable(@PathVariable Integer id, @RequestParam Integer newTable) {
        return ResponseEntity.ok(comandaService.transferTab(id, newTable));
    }

    @PatchMapping("/pay-group")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> payGroupedTabs(
            @RequestParam List<Integer> tabIds, 
            @RequestParam BigDecimal amountReceived) {
        return ResponseEntity.ok(comandaService.payGroupedTabs(tabIds, amountReceived));
    }

    @PatchMapping("/{id}/remove-couvert")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> removeCoverCharge(@PathVariable Integer id) {
        comandaService.removeCoverCharge(id);
        return ResponseEntity.ok("Couvert removido apenas desta comanda.");
    }
}
