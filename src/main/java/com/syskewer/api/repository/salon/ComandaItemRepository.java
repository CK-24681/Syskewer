package com.syskewer.api.repository.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syskewer.api.model.salon.ComandaItem;
import com.syskewer.api.model.salon.PrepStatus;

public interface ComandaItemRepository extends JpaRepository<ComandaItem, Long> {
    List<ComandaItem> findByComandaId(Integer comandaId);
    List<ComandaItem> findByPrepStatusInOrderByCreatedAtAsc(List<PrepStatus> statuses);
}
