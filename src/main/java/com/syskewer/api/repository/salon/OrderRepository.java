package com.syskewer.api.repository.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syskewer.api.model.salon.Order;
import com.syskewer.api.model.salon.PrepStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBillId(Integer billId);
    List<Order> findByPrepStatusInOrderByCreatedAtAsc(List<PrepStatus> statuses);
}
