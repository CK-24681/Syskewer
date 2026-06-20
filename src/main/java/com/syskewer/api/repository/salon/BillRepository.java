package com.syskewer.api.repository.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syskewer.api.model.salon.Bill;
import com.syskewer.api.model.salon.BillStatus;

public interface BillRepository extends JpaRepository<Bill, Integer> {
    List<Bill> findByStatus(BillStatus status);

    @Query("SELECT b FROM Bill b JOIN b.tables t WHERE t.id = :tableId AND b.status = :status")
    List<Bill> findByTableIdAndStatus(@Param("tableId") Integer tableId, @Param("status") BillStatus status);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Bill b JOIN b.tables t WHERE t.id = :tableId AND b.status = :status")
    boolean existsByTableIdAndStatus(@Param("tableId") Integer tableId, @Param("status") BillStatus status);
}
