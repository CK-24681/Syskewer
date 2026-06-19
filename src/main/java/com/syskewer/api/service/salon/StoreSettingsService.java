package com.syskewer.api.service.salon;

import org.springframework.stereotype.Service;

import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.store.StoreSettings;
import com.syskewer.api.repository.store.StoreSettingsRepository;

@Service
public class StoreSettingsService {

    private final StoreSettingsRepository repository;

    public StoreSettingsService(StoreSettingsRepository repository) {
        this.repository = repository;
    }

    public StoreSettings getSettings() {
        return repository.findById(1)
                .orElseThrow(() -> new ResourceNotFoundException("Configurações da loja não encontradas! O banco de dados foi inicializado corretamente?"));
    }

    public StoreSettings toggleStore() {
        StoreSettings settings = getSettings();
        settings.setIsOpen(!settings.getIsOpen());
        return repository.save(settings);
    }

    public Boolean isStoreOpen() {
        return getSettings().getIsOpen();
    }
}