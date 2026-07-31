package com.example.zadel.repository;

import com.example.zadel.model.SprCountry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** Репозиторий для работы со справочником стран */
@Repository
public interface SprCountryRepository extends JpaRepository<SprCountry, UUID> {
}