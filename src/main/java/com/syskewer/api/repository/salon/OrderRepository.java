package com.syskewer.api.repository.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.PrepStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByTabId(Integer tabId);
    List<Order> findByPrepStatusInOrderByCreatedAtAsc(List<PrepStatus> statuses);
}
