package com.example.zadel.repository;

import com.example.zadel.model.RegCustomerRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface RegCustomerRatingRepository extends JpaRepository<RegCustomerRating, UUID> {

    List<RegCustomerRating> findByCustomerUidOrderByCreatedAtDesc(UUID customerUid);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM RegCustomerRating r WHERE r.customer.uid = :customerUid")
    Double getAverageRatingByCustomerUid(UUID customerUid);

    @Modifying
    @Transactional
    @Query("DELETE FROM RegCustomerRating r WHERE r.customer.uid = :customerUid")
    void deleteByCustomerUid(UUID customerUid);
}