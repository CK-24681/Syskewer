package com.syskewer.api.repository.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syskewer.api.model.salon.ComandaItemDetail;

public interface ComandaItemDetailRepository extends JpaRepository<ComandaItemDetail, Long> {
    List<ComandaItemDetail> findByComandaItemId(Long comandaItemId);
}
