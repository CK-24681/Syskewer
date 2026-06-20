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

import com.syskewer.api.dto.salon.BillPartialDto;
import com.syskewer.api.dto.salon.BillPaymentDto;
import com.syskewer.api.dto.salon.BillSummaryDto;
import com.syskewer.api.dto.salon.BillUpdateDto;
import com.syskewer.api.dto.salon.BillOpenDto;
import com.syskewer.api.dto.salon.BillDeliveryDto;
import com.syskewer.api.service.salon.BillService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bills")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Bills", description = "Operations for opening, paying, and closing bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    // List open bills in the salon
    @GetMapping("/open")
    public ResponseEntity<List<BillSummaryDto>> getOpenTabs() {
        return ResponseEntity.ok(billService.getOpenTabs());
    }

    // Return consumption breakdown details of a bill
    @GetMapping("/{id}/partial")
    public ResponseEntity<BillPartialDto> getTabPartial(@PathVariable Integer id) {
        return ResponseEntity.ok(billService.getTabPartial(id));
    }

    // Close bill releasing the table
    @PatchMapping("/{id}/close")
    public ResponseEntity<String> closeTab(@PathVariable Integer id) {
        billService.closeTab(id);
        return ResponseEntity.ok("Payment received successfully! The bill is closed and the table is free.");
    }

    // Apply cover charge to the bill
    @PatchMapping("/{id}/couvert")
    public ResponseEntity<String> toggleCoverCharge(@PathVariable Integer id) {
        billService.toggleCoverCharge(id);
        return ResponseEntity.ok("Cover charge processed successfully on table/bill!");
    }

    // Open a new delivery bill
    @PostMapping("/delivery")
    public ResponseEntity<BillSummaryDto> openDeliveryTab(
            @RequestBody @Valid BillDeliveryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.openDeliveryTab(dto));
    }

    // Archive bill as deferred debt
    @PatchMapping("/{id}/fiado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<String> archiveAsCredit(@PathVariable Integer id, @RequestParam String doc) {
        billService.archiveAsCredit(id, doc);
        return ResponseEntity.ok("Bill archived as deferred debt. Table free!");
    }

    // Open a new table or bar bill
    @PostMapping
    public ResponseEntity<BillSummaryDto> openTab(
            @RequestBody @Valid BillOpenDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.openTab(dto));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<BillSummaryDto> patchTab(@PathVariable Integer id, @RequestBody BillUpdateDto dto) {
        return ResponseEntity.ok(billService.patchTab(id, dto));
    }

    // Register partial or final payment of a bill
    @PatchMapping("/{id}/pay")
    public ResponseEntity<String> registerPayment(@PathVariable Integer id, @RequestBody @Valid BillPaymentDto dto) {
        String message = billService.registerPayment(id, dto.amount(), dto.discount());
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<Void> cancelTab(@PathVariable Integer id) {
        billService.cancelTab(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> transferTable(@PathVariable Integer id, @RequestParam Integer newTable) {
        return ResponseEntity.ok(billService.transferTab(id, newTable));
    }

    @PatchMapping("/pay-group")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> payGroupedTabs(
            @RequestParam List<Integer> tabIds, 
            @RequestParam BigDecimal amountReceived) {
        return ResponseEntity.ok(billService.payGroupedTabs(tabIds, amountReceived));
    }

    @PatchMapping("/{id}/remove-couvert")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GARCOM')")
    public ResponseEntity<String> removeCoverCharge(@PathVariable Integer id) {
        billService.removeCoverCharge(id);
        return ResponseEntity.ok("Cover charge removed from this bill.");
    }
}
