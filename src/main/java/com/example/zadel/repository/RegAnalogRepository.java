// RegAnalogRepository.java
package com.example.zadel.repository;

import com.example.zadel.model.RegAnalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

public interface RegAnalogRepository extends JpaRepository<RegAnalog, UUID> {

    List<RegAnalog> findByMaterialUid(UUID materialUid);

    @Modifying
    @Transactional
    @Query("DELETE FROM RegAnalog r WHERE r.material.uid = :materialUid")
    void deleteByMaterialUid(@Param("materialUid") UUID materialUid);
}