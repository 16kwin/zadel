package com.example.zadel.repository;

import com.example.zadel.model.SprCustomerImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface SprCustomerImageRepository extends JpaRepository<SprCustomerImage, UUID> {

    List<SprCustomerImage> findByCustomerUidOrderBySortOrderAsc(UUID customerUid);

    @Modifying
    @Transactional
    @Query("DELETE FROM SprCustomerImage i WHERE i.customer.uid = :customerUid")
    void deleteByCustomerUid(UUID customerUid);
}