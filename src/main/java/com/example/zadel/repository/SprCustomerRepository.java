package com.example.zadel.repository;

import com.example.zadel.model.RegCustomers;
import com.example.zadel.model.SprCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SprCustomerRepository extends JpaRepository<SprCustomer, UUID> {

    @Query("SELECT COALESCE(MAX(s.code), 0) FROM SprCustomer s")
    Integer findMaxCode();
    
}