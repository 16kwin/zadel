package com.example.zadel.repository;

import com.example.zadel.model.SprManufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** Репозиторий для работы со справочником производителей */
@Repository
public interface SprManufacturerRepository extends JpaRepository<SprManufacturer, UUID> {
}