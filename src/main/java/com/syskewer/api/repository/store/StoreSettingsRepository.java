package com.syskewer.api.repository.store;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syskewer.api.model.store.StoreSettings;

public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Integer> {
}
