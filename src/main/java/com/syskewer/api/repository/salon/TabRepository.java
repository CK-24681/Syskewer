package com.syskewer.api.repository.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syskewer.api.model.salon.Tab;
import com.syskewer.api.model.salon.TabStatus;

@Repository
public interface TabRepository extends JpaRepository<Tab, Integer> {
    List<Tab> findByStatus(TabStatus status);
}
