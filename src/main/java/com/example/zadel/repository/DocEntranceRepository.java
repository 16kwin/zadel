package com.example.zadel.repository;

import com.example.zadel.model.DocEntrance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** Репозиторий для работы с документами поступления материалов */
@Repository
public interface DocEntranceRepository extends JpaRepository<DocEntrance, UUID> {
}