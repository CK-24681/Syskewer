package com.syskewer.api.repository.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syskewer.api.model.salon.OrderDetail;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByOrderId(Long orderId);
}
