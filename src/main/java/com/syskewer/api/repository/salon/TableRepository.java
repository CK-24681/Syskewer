package com.syskewer.api.repository.salon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syskewer.api.model.salon.Table;

public interface TableRepository extends JpaRepository<Table, Integer> {
    boolean existsByNumber(Integer number);
    Optional<Table> findByNumber(Integer number);
}
