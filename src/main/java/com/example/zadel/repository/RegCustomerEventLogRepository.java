// RegSupplierEventLogRepository.java
package com.example.zadel.repository;

import com.example.zadel.model.RegCustomerEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RegCustomerEventLogRepository extends JpaRepository<RegCustomerEventLog, UUID> {

    List<RegCustomerEventLog> findByCustomerUidOrderByCreatedAtDesc(UUID customerUid);
}