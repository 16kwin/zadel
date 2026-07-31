package com.example.zadel.repository;

import com.example.zadel.model.SprCustomerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface SprCustomerDocumentRepository extends JpaRepository<SprCustomerDocument, UUID> {

    List<SprCustomerDocument> findByCustomerUidOrderByCreatedAtDesc(UUID customerUid);

    @Modifying
    @Transactional
    @Query("DELETE FROM SprCustomerDocument d WHERE d.customer.uid = :customerUid")
    void deleteByCustomerUid(UUID customerUid);
}