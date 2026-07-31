// ЗАДЕЛ — repository/OrderTrackingRepository.java
package com.example.zadel.repository;

import com.example.zadel.model.OrderTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Long> {
    List<OrderTracking> findByOrderUidOrderByDatetimeDesc(String orderUid);
}