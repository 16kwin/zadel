package com.example.zadel.repository;

import com.example.zadel.model.RegEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RegEventLogRepository extends JpaRepository<RegEventLog, UUID> {
    List<RegEventLog> findByMaterialUidOrderByCreatedAtDesc(UUID materialUid);
}