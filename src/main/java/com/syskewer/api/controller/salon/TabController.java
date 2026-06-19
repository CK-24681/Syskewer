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

import com.syskewer.api.dto.salon.TabPartialDto;
import com.syskewer.api.dto.salon.TabPaymentDto;
import com.syskewer.api.dto.salon.TabSummaryDto;
import com.syskewer.api.service.salon.TabService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/tabs")
public class TabController {

    private final TabService tabService;

    public TabController(TabService tabService) {
        this.tabService = tabService;
    }

    /** @return comandas abertas no salão */
    @GetMapping("/open")
    public ResponseEntity<List<TabSummaryDto>> getOpenTabs() {
        return ResponseEntity.ok(tabService.getOpenTabs());
    }

    /** @param id id da comanda */
    @GetMapping("/{id}/partial")
    public ResponseEntity<TabPartialDto> getTabPartial(@PathVariable Integer id) {
        return ResponseEntity.ok(tabService.getTabPartial(id));
    }

    /** @param id id da comanda */
    @PatchMapping("/{id}/close")
    public ResponseEntity<String> closeTab(@PathVariable Integer id) {
        tabService.closeTab(id);
        return ResponseEntity.ok("Conta recebida com sucesso! A comanda foi fechada e a mesa está livre.");
    }

    /** Couvert artístico fixo rateado pela mesa. */
    @PatchMapping("/{id}/couvert")
    public ResponseEntity<String> toggleCoverCharge(@PathVariable Integer id) {
        tabService.toggleCoverCharge(id);
        return ResponseEntity.ok("Couvert artístico processado com sucesso na mesa/comanda!");
    }

    /** Taxa de entrega fixa. */
    @PostMapping("/delivery")
    public ResponseEntity<com.syskewer.api.dto.salon.TabSummaryDto> openDeliveryTab(
            @RequestBody @Valid com.syskewer.api.dto.salon.TabDeliveryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tabService.openDeliveryTab(dto));
    }

    /** @param doc CPF ou telefone do devedor */
    @PatchMapping("/{id}/fiado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<String> archiveAsCredit(@PathVariable Integer id, @RequestParam String doc) {
        tabService.archiveAsCredit(id, doc);
        return ResponseEntity.ok("Comanda arquivada como Fiado/Inadimplente. Mesa liberada!");
    }

    /** @param dto dados de abertura no salão */
    @PostMapping
    public ResponseEntity<com.syskewer.api.dto.salon.TabSummaryDto> openTab(
            @RequestBody @Valid com.syskewer.api.dto.salon.TabOpenDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tabService.openTab(dto));
    }

    /** @param dto valor pago (parcial ou total) */
    @PatchMapping("/{id}/pay")
    public ResponseEntity<String> registerPayment(@PathVariable Integer id, @RequestBody @Valid TabPaymentDto dto) {
        String message = tabService.registerPayment(id, dto.amount(), dto.discount());
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<Void> cancelTab(@PathVariable Integer id) {
        tabService.cancelTab(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> transferTable(@PathVariable Integer id, @RequestParam Integer newTable) {
        return ResponseEntity.ok(tabService.transferTab(id, newTable));
    }

    @PatchMapping("/pay-group")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> payGroupedTabs(
            @RequestParam List<Integer> tabIds, 
            @RequestParam BigDecimal amountReceived) {
        return ResponseEntity.ok(tabService.payGroupedTabs(tabIds, amountReceived));
    }

    @PatchMapping("/{id}/remove-couvert")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> removeCoverCharge(@PathVariable Integer id) {
        tabService.removeCoverCharge(id);
        return ResponseEntity.ok("Couvert removido apenas desta comanda.");
    }
}
