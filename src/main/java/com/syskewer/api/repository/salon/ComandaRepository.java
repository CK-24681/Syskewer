package com.syskewer.api.repository.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syskewer.api.model.salon.Comanda;
import com.syskewer.api.model.salon.ComandaStatus;

public interface ComandaRepository extends JpaRepository<Comanda, Integer> {
    List<Comanda> findByStatus(ComandaStatus status);

    @Query("SELECT c FROM Comanda c JOIN c.tables t WHERE t.id = :tableId AND c.status = :status")
    List<Comanda> findByTableIdAndStatus(@Param("tableId") Integer tableId, @Param("status") ComandaStatus status);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Comanda c JOIN c.tables t WHERE t.id = :tableId AND c.status = :status")
    boolean existsByTableIdAndStatus(@Param("tableId") Integer tableId, @Param("status") ComandaStatus status);
}
