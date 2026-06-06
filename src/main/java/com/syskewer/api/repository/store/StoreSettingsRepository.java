package com.syskewer.api.repository.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syskewer.api.model.store.StoreSettings;

@Repository
public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Integer> {
}
