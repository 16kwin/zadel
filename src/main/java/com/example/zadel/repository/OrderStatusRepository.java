// ЗАДЕЛ — repository/OrderStatusRepository.java
package com.example.zadel.repository;

import com.example.zadel.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusRepository extends JpaRepository<OrderStatus, Long> {
    List<OrderStatus> findByOrderUidOrderByDatetimeDesc(String orderUid);
}