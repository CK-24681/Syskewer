package com.syskewer.api.controller.salon;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syskewer.api.model.store.StoreSettings;
import com.syskewer.api.service.salon.StoreSettingsService;

@RestController
@RequestMapping("/store")
public class StoreSettingsController {

    private final StoreSettingsService service;

    public StoreSettingsController(StoreSettingsService service) {
        this.service = service;
    }

    // Alterna o status de funcionamento do bar (aberto/fechado)
    @PostMapping("/toggle")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StoreSettings> toggleStore() {
        StoreSettings settings = service.toggleStore();
        return ResponseEntity.ok(settings);
    }
}
