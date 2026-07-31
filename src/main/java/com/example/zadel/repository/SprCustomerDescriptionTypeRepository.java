package com.example.zadel.repository;

import com.example.zadel.model.SprCustomerDescriptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SprCustomerDescriptionTypeRepository extends JpaRepository<SprCustomerDescriptionType, UUID> {
}