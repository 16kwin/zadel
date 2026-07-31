package com.example.zadel.repository;

import com.example.zadel.model.SprMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** Репозиторий для работы со справочником единиц измерения */
@Repository
public interface SprMeasureRepository extends JpaRepository<SprMeasure, UUID> {
}