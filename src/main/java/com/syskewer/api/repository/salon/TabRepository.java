package com.syskewer.api.repository.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syskewer.api.model.salon.Tab;
import com.syskewer.api.model.salon.TabStatus;

public interface TabRepository extends JpaRepository<Tab, Integer> {
    List<Tab> findByStatus(TabStatus status);
    List<Tab> findByTableIdAndStatus(Integer tableId, TabStatus status);
    boolean existsByTableIdAndStatus(Integer tableId, TabStatus status);
}
