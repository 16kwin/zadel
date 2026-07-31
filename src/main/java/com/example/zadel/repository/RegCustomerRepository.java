package com.example.zadel.repository;

import com.example.zadel.model.RegCustomers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface RegCustomerRepository extends JpaRepository<RegCustomers, UUID> {

    List<RegCustomers> findByMaterialUid(UUID materialUid);

    List<RegCustomers> findByCustomerUid(UUID customerUid);

    @Modifying
    @Transactional
    @Query("DELETE FROM RegCustomers r WHERE r.material.uid = :materialUid")
    void deleteByMaterialUid(@Param("materialUid") UUID materialUid);
}